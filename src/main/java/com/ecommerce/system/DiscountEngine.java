package com.ecommerce.system;

import com.ecommerce.model.Cart;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import java.util.Map;

// Task 9: Discount & Coupon Engine
public class DiscountEngine {
    
    public static double calculateDiscountedTotal(Cart cart, ProductRepository productRepo, String couponCode) {
        double subtotal = 0.0;
        double itemDiscounts = 0.0;
        
        // 1. Quantity > 3 extra 5% off
        for (Map.Entry<String, Integer> entry : cart.getItems().entrySet()) {
            Product p = productRepo.getProduct(entry.getKey());
            if (p != null) {
                double lineTotal = p.getPrice() * entry.getValue();
                subtotal += lineTotal;
                
                if (entry.getValue() > 3) {
                    itemDiscounts += (lineTotal * 0.05); // 5% off this product specifically
                }
            }
        }
        
        double totalWithItemDiscounts = subtotal - itemDiscounts;
        double finalTotal = totalWithItemDiscounts;

        // 2. Global discounts - only one global discount/coupon can apply
        
        double couponDiscount = 0.0;
        if ("SAVE10".equalsIgnoreCase(couponCode)) {
            couponDiscount = totalWithItemDiscounts * 0.10;
        } else if ("FLAT200".equalsIgnoreCase(couponCode)) {
            couponDiscount = 200.0;
        }
        
        // Total threshold discount
        double thresholdDiscount = 0.0;
        if (totalWithItemDiscounts > 1000.0) {
            thresholdDiscount = totalWithItemDiscounts * 0.10; // 10% discount
        }
        
        // Apply the best between the generic Threshold vs User Coupon to avoid invalid combo stacking
        double bestGlobalDiscount = Math.max(couponDiscount, thresholdDiscount);
        
        finalTotal -= bestGlobalDiscount;

        // Prevent negative totals
        return Math.max(0.0, finalTotal);
    }
}
