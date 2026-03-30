package com.ecommerce.system;

import java.util.Random;

// Task 18: Failure Injection System
public class FailureInjector {
    private static boolean injectionEnabled = false;
    private static final Random random = new Random();

    public static void toggle() {
        injectionEnabled = !injectionEnabled;
        System.out.println("Failure Injector is now " + (injectionEnabled ? "ENABLED" : "DISABLED"));
    }

    public static boolean shouldFail(String operation) {
        if (!injectionEnabled) return false;
        
        // Randomly fail 1/3 of the time if enabled
        boolean fails = random.nextInt(3) == 0;
        if (fails) {
            System.err.println("--- INJECTED ALARM: Synthetic Failure in operation: " + operation + " ---");
        }
        return fails;
    }
    
    public static boolean isInjectionEnabled() {
        return injectionEnabled;
    }
}
