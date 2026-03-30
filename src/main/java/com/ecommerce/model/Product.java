package com.ecommerce.model;

import java.util.concurrent.locks.ReentrantLock;

public class Product {
    private String id;
    private String name;
    private double price;
    private int totalStock;
    private int reservedStock; // Stock in people's carts
    
    // Logical lock for concurrency control
    private final ReentrantLock lock = new ReentrantLock();

    public Product(String id, String name, double price, int totalStock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.totalStock = totalStock;
        this.reservedStock = 0;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(int totalStock) {
        this.totalStock = totalStock;
    }

    public int getReservedStock() {
        return reservedStock;
    }

    public void setReservedStock(int reservedStock) {
        this.reservedStock = reservedStock;
    }
    
    public int getAvailableStock() {
        return totalStock - reservedStock;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    // Reservation atomic operations
    public boolean reserve(int amount) {
        lock.lock();
        try {
            if (getAvailableStock() >= amount) {
                reservedStock += amount;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void unreserve(int amount) {
        lock.lock();
        try {
            reservedStock -= amount;
            if (reservedStock < 0) {
                reservedStock = 0;
            }
        } finally {
            lock.unlock();
        }
    }
    
    // Real deduction upon order placement
    public void deductStock(int amount) {
        lock.lock();
        try {
            totalStock -= amount;
            reservedStock -= amount; // Deduct the reserved stock as it's now sold
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return String.format("Product[id=%s, name=%s, price=$%.2f, totalStock=%d, reservedStock=%d, available=%d]", 
            id, name, price, totalStock, reservedStock, getAvailableStock());
    }
}
