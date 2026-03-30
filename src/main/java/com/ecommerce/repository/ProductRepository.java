package com.ecommerce.repository;

import com.ecommerce.model.Product;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProductRepository {
    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public void addProduct(Product product) {
        products.put(product.getId(), product);
    }

    public Product getProduct(String id) {
        return products.get(id);
    }

    public Collection<Product> getAllProducts() {
        return products.values();
    }
    
    public boolean exists(String id) {
        return products.containsKey(id);
    }
}
