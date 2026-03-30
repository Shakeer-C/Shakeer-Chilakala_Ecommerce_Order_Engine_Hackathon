package com.ecommerce.repository;

import com.ecommerce.model.Order;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OrderRepository {
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public void save(Order order) {
        orders.put(order.getId(), order);
    }

    public Order getOrder(String id) {
        return orders.get(id);
    }

    public Collection<Order> getAllOrders() {
        return orders.values();
    }
}
