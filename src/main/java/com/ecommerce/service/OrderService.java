package com.ecommerce.service;

import com.ecommerce.model.Cart;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Product;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.system.EventBus;
import com.ecommerce.system.EventBus.EventType;
import com.ecommerce.system.FailureInjector;
import com.ecommerce.system.FraudDetectionService;
import com.ecommerce.system.AuditLogger;
import com.ecommerce.system.DiscountEngine;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class OrderService {
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final FraudDetectionService fraudService;
    private final EventBus eventBus;

    /**
     * Cache to prevent double order placement from rapid UI clicks or network retries.
     * Maps UserId + CartHashCode to a 'processed' state.
     */
    private final Set<String> processedCarts = new HashSet<>();

    public OrderService(CartService cartService, ProductRepository productRepository,
            OrderRepository orderRepository, PaymentService paymentService,
            FraudDetectionService fraudService, EventBus eventBus) {
        this.cartService = cartService;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.fraudService = fraudService;
        this.eventBus = eventBus;
    }

    public Order placeOrder(String userId, String couponCode) {
        Cart cart = cartService.getCart(userId);
        if (cart.getItems().isEmpty()) {
            System.out.println("Cart is empty. Cannot place order.");
            return null;
        }

        // Idempotency check
        String idempotencyKey = userId + "_" + cart.getItems().hashCode();
        if (processedCarts.contains(idempotencyKey)) {
            System.out.println("Order already processed for this exact cart state (Idempotency trigger)!");
            return null;
        }

        double totalAmount = DiscountEngine.calculateDiscountedTotal(cart, productRepository, couponCode);

        // Fraud check - verify user is not flagged before proceeding
        if (fraudService.isFraudulent(userId, totalAmount)) {
            System.out.println("ALERT: Transaction blocked for user " + userId + " by Fraud Detection System.");
            return null;
        }

        // 4. Create order
        String orderId = UUID.randomUUID().toString().substring(0, 8);
        Order order = new Order(orderId, userId, cart.getItems(), totalAmount);

        if (FailureInjector.shouldFail("Order Creation")) {
            System.out.println("System Failure: Could not create order.");
            return null;
        }

        orderRepository.save(order);
        processedCarts.add(idempotencyKey); // Mark this cart state as handled

        AuditLogger.log("Order " + orderId + " successfully created for " + userId + " ($" + totalAmount + ")");
        eventBus.publish(EventType.ORDER_CREATED, orderId);

        // 5. Clear cart but preserve reservations for async payment
        cart.getItems().clear();
        cart.getItemTimestamps().clear();

        advanceOrderStatus(order, OrderStatus.PENDING_PAYMENT);

        return processPayment(order);
    }

    private Order processPayment(Order order) {
        System.out.println("Processing payment for Order " + order.getId() + "...");

        boolean success = paymentService.processPayment(order);

        if (success) {
            System.out.println("Payment SUCCESSFUL for Order " + order.getId());
            advanceOrderStatus(order, OrderStatus.PAID);
            eventBus.publish(EventType.PAYMENT_SUCCESS, order.getId());

            // Finalize inventory decrement now that payment is confirmed
            for (Map.Entry<String, Integer> item : order.getOrderedItems().entrySet()) {
                Product product = productRepository.getProduct(item.getKey());
                if (product != null) {
                    if (FailureInjector.shouldFail("Inventory Update")) {
                        // FIXME: This leaves the system in an inconsistent state. Need a proper rollback or compensator here.
                        System.err.println("CRITICAL: Inventory sync failed for product " + product.getId() + " after successful payment!");
                    } else {
                        product.deductStock(item.getValue());
                        eventBus.publish(EventType.INVENTORY_UPDATED, product.getId());
                    }
                }
            }
            advanceOrderStatus(order, OrderStatus.SHIPPED); 
            return order;
        } else {
            System.out.println("Payment FAILED for Order " + order.getId());
            rollbackOrder(order);
            return order;
        }
    }

    private void rollbackOrder(Order order) {
        System.out.println("Rolling back Order " + order.getId() + "...");
        advanceOrderStatus(order, OrderStatus.FAILED);

        // Return all items to the available stock pool
        for (Map.Entry<String, Integer> item : order.getOrderedItems().entrySet()) {
            Product product = productRepository.getProduct(item.getKey());
            if (product != null) {
                product.unreserve(item.getValue()); 
            }
        }
        AuditLogger.log("ROLLBACK: Restored stock for cancelled/failed order " + order.getId());
    }

    // Task 12: Order Cancellation Engine
    public boolean cancelOrder(String orderId) {
        Order order = orderRepository.getOrder(orderId);
        if (order == null)
            return false;

        if (order.getStatus() == OrderStatus.CANCELLED) {
            System.out.println("Edge Case: Cannot cancel already cancelled order!");
            return false;
        }
        if (order.getStatus() == OrderStatus.DELIVERED) {
            System.out.println("Cannot cancel delivered order.");
            return false;
        }

        advanceOrderStatus(order, OrderStatus.CANCELLED);
        // Refund & Restore
        for (Map.Entry<String, Integer> entry : order.getOrderedItems().entrySet()) {
            Product p = productRepository.getProduct(entry.getKey());
            if (p != null) {
                p.setTotalStock(p.getTotalStock() + entry.getValue());
                AuditLogger.log("Restored " + entry.getValue() + " units of " + p.getName() + " due to cancellation.");
            }
        }
        return true;
    }

    // Task 13: Return & Refund System
    public boolean processReturn(String orderId, String productId, int qty) {
        Order order = orderRepository.getOrder(orderId);
        if (order == null)
            return false;
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.SHIPPED) {
            System.out.println("Can only return shipped/delivered items.");
            return false;
        }

        Integer orderedQty = order.getOrderedItems().get(productId);
        if (orderedQty == null || orderedQty < qty) {
            System.out.println("Cannot return more than ordered.");
            return false;
        }

        Product p = productRepository.getProduct(productId);
        if (p != null) {
            p.setTotalStock(p.getTotalStock() + qty);
            System.out.println("Refund processed. Stock restored for " + p.getName());
            AuditLogger.log("Partial Return: " + qty + " of " + productId + " from Order " + orderId);
            return true;
        }
        return false;
    }

    public void advanceOrderStatus(Order order, OrderStatus nextState) {
        OrderStatus current = order.getStatus();
        boolean valid = isValidTransition(current, nextState);

        if (valid) {
            order.setStatus(nextState);
            System.out.println("Order " + order.getId() + " state transitioned: " + current + " -> " + nextState);
        } else {
            System.out.println(
                    "Invalid state transition for Order " + order.getId() + ": " + current + " -> " + nextState);
        }
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus next) {
        switch (current) {
            case CREATED: return next == OrderStatus.PENDING_PAYMENT || next == OrderStatus.CANCELLED;
            case PENDING_PAYMENT: return next == OrderStatus.PAID || next == OrderStatus.FAILED || next == OrderStatus.CANCELLED;
            case PAID: return next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED: return next == OrderStatus.DELIVERED || next == OrderStatus.CANCELLED;
            case FAILED: return false; 
            case CANCELLED: return false; 
            case DELIVERED: return false; 
            default: return false;
        }
    }
}