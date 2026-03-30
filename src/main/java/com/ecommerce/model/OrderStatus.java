package com.ecommerce.model;

public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    DELIVERED,
    FAILED,
    CANCELLED
}
