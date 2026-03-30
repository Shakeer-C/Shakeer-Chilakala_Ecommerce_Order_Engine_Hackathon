package com.ecommerce.service;

import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import java.util.Collection;

public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Task 1: Add new products, prevent duplicates, handle stock
    public boolean addProduct(String id, String name, double price, int stock) {
        if (stock < 0) {
            System.out.println("Error: Stock cannot be negative.");
            return false;
        }
        if (productRepository.exists(id)) {
            System.out.println("Error: Product ID " + id + " already exists.");
            return false;
        }
        
        Product p = new Product(id, name, price, stock);
        productRepository.addProduct(p);
        return true;
    }

    public Collection<Product> getAllProducts() {
        return productRepository.getAllProducts();
    }

    public Product getProduct(String id) {
        return productRepository.getProduct(id);
    }

    // Task 10: Inventory Alert System
    public void printLowStockAlerts() {
        System.out.println("--- Low Stock Alerts ---");
        boolean found = false;
        for (Product p : productRepository.getAllProducts()) {
            if (p.getAvailableStock() == 0) {
                System.out.println("[URGENT] Out of Stock: " + p.getName());
                found = true;
            } else if (p.getAvailableStock() <= 3) {
                System.out.println("[WARNING] Low Stock (" + p.getAvailableStock() + " left): " + p.getName());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Inventory levels are healthy.");
        }
    }
}
