package com.ecommerce.repository;

import com.ecommerce.model.Cart;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CartRepository {
    private final Map<String, Cart> carts = new ConcurrentHashMap<>();

    public Cart getCart(String userId) {
        // Compute if absent to ensure no duplicate carts for same user
        return carts.computeIfAbsent(userId, k -> new Cart(k));
    }

    public Collection<Cart> getAllCarts() {
        return carts.values();
    }
}

    