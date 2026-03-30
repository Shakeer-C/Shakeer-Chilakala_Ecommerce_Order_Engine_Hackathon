package com.ecommerce.model;

import java.util.Map;
import java.util.HashMap;

public class Order {
    private String id;
    private String userId;
    private Map<String, Integer> orderedItems;
    private double totalAmount;
    private OrderStatus status;

    public Order(String id, String userId, Map<String, Integer> originalCartItems, double totalAmount) {
        this.id = id;
        this.userId = userId;
        this.orderedItems = new HashMap<>(originalCartItems);
        this.totalAmount = totalAmount;
        this.status = OrderStatus.CREATED;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, Integer> getOrderedItems() {
        return orderedItems;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Order[id=%s, user=%s, total=$%.2f, status=%s, items=%s]",
                id, userId, totalAmount, status, orderedItems);
    }
}
