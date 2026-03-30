package com.ecommerce.system;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

// Task 17: Fraud Detection System
public class FraudDetectionService {
    // UserId -> List of order timestamps in ms
    private final Map<String, List<Long>> orderHistory = new ConcurrentHashMap<>();
    
    public boolean isFraudulent(String userId, double orderValue) {
        if (orderValue > 10000.0) { // High value order threshold
            AuditLogger.log("FRAUD ALERT: User " + userId + " attempted suspiciously high-value order.");
            return true;
        }

        List<Long> times = orderHistory.computeIfAbsent(userId, k -> new ArrayList<>());
        long now = System.currentTimeMillis();
        
        // Keep only orders from the last 1 minute (60,000 ms)
        synchronized (times) {
            times.removeIf(time -> (now - time) > 60000);
            
            // If they already have 3 orders in the last minute (meaning this is the 4th)
            if (times.size() >= 3) {
                AuditLogger.log("FRAUD ALERT: User " + userId + " placed >3 orders in 1 minute. Flagged!");
                return true;
            }
            
            times.add(now);
        }
        return false;
    }
}
