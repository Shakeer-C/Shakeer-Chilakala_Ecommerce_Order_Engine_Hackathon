package com.ecommerce.service;

import com.ecommerce.model.Cart;
import com.ecommerce.model.Product;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.system.AuditLogger;
import java.util.Map;
import java.util.Iterator;

/**
 * Service for managing user shopping carts and stock reservations.
 */
public class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        
        // Setup a background thread to handle item reservation timeouts
        Thread evictor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(10000); // Check every 10s
                    evictExpiredReservations();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        evictor.setDaemon(true);
        evictor.start();
    }

    private void evictExpiredReservations() {
        long now = System.currentTimeMillis();
        // Item reservations expire after 30 seconds for test purposes
        long expiryTimeMs = 30000; 
        
        for (Cart cart : cartRepository.getAllCarts()) {
            Iterator<Map.Entry<String, Long>> it = cart.getItemTimestamps().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> cartItem = it.next();
                if (now - cartItem.getValue() > expiryTimeMs) {
                    String productId = cartItem.getKey();
                    int quantity = cart.getItems().get(productId);
                    
                    Product product = productRepository.getProduct(productId);
                    if (product != null) {
                        product.unreserve(quantity);
                        AuditLogger.log("SYSTEM: Released " + quantity + " units of " + product.getName() + " due to reservation timeout.");
                    }
                    
                    cart.getItems().remove(productId);
                    it.remove();
                }
            }
        }
    }

    public Cart getCart(String userId) {
        return cartRepository.getCart(userId);
    }

    // Processes 'Add to Cart' with real-time stock reservation
    public boolean addToCart(String userId, String productId, int quantity) {
        if (quantity <= 0)
            return false;

        Product product = productRepository.getProduct(productId);
        if (product == null) {
            System.out.println("Product not found.");
            return false;
        }

        // Logical locking on Product
        boolean reserved = product.reserve(quantity);
        if (reserved) {
            Cart cart = getCart(userId);
            cart.getItems().merge(productId, quantity, Integer::sum);
            cart.getItemTimestamps().put(productId, System.currentTimeMillis());
            AuditLogger.log("USER: " + userId + " reserved " + quantity + " units of " + productId);
            return true;
        } else {
            System.out.println("Insufficient stock for: " + product.getName() + " (Available: " + product.getAvailableStock() + ")");
            return false;
        }
    }

    public boolean removeFromCart(String userId, String productId, int quantity) {
        Cart cart = getCart(userId);
        Integer currentQty = cart.getItems().get(productId);

        if (currentQty == null) {
            return false;
        }

        int removeQty = Math.min(quantity, currentQty);

        Product product = productRepository.getProduct(productId);
        if (product != null) {
            product.unreserve(removeQty);
        }

        if (currentQty - removeQty <= 0) {
            cart.getItems().remove(productId);
        } else {
            cart.getItems().put(productId, currentQty - removeQty);
        }
        return true;
    }

    public void clearCart(String userId) {
        Cart cart = getCart(userId);
        // Ensure we release all active stock reservations before clearing the cart
        for (Map.Entry<String, Integer> cartItem : cart.getItems().entrySet()) {
            Product product = productRepository.getProduct(cartItem.getKey());
            if (product != null) {
                product.unreserve(cartItem.getValue());
            }
        }
        cart.clear();
    }
}