package com.ecommerce.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Cart {
    private String userId;
    // Map of ProductId -> Quantity
    private Map<String, Integer> items;
    // Map of ProductId -> Timestamp added
    private Map<String, Long> itemTimestamps;

    public Cart(String userId) {
        this.userId = userId;
        this.items = new ConcurrentHashMap<>();
        this.itemTimestamps = new ConcurrentHashMap<>();
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, Integer> getItems() {
        return items;
    }

    public void clear() {
        items.clear();
        itemTimestamps.clear();
    }

    public Map<String, Long> getItemTimestamps() {
        return itemTimestamps;
    }
}


    