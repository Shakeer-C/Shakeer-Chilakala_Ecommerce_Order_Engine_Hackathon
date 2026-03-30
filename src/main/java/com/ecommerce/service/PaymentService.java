package com.ecommerce.service;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.system.EventBus;
import com.ecommerce.system.FailureInjector;
import java.util.Random;

public class PaymentService {
    private final Random random = new Random();
    
    public boolean processPayment(Order order) {
        if (FailureInjector.isInjectionEnabled() && FailureInjector.shouldFail("Payment Integration")) {
            System.err.println("[PaymentService] Simulated systemic payment failure occurred!");
            return false;
        }

        try {
            Thread.sleep(800); // Network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 80% genuine success rate
        return random.nextDouble() > 0.2;
    }
}
