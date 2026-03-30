package com.ecommerce;

import com.ecommerce.model.Product;
import com.ecommerce.model.Cart;
import com.ecommerce.model.Order;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.CartService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.PaymentService;
import com.ecommerce.system.EventBus;
import com.ecommerce.system.FailureInjector;
import com.ecommerce.system.FraudDetectionService;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CLIApplication {

    private static ProductRepository productRepo = new ProductRepository();
    private static CartRepository cartRepo = new CartRepository();
    private static OrderRepository orderRepo = new OrderRepository();

    private static EventBus eventBus = new EventBus();
    private static FraudDetectionService fraudService = new FraudDetectionService();
    private static PaymentService paymentService = new PaymentService();

    private static ProductService productService = new ProductService(productRepo);
    private static CartService cartService = new CartService(cartRepo, productRepo);
    private static OrderService orderService = new OrderService(cartService, productRepo, orderRepo, paymentService,
            fraudService, eventBus);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String currentUserId = "shakeer123";
        String activeCouponCode = null;

        System.out.println("*****************---------------------****************");
        System.out.println("   E-Commerce Distributed Backend Simulator      ");
        System.out.println("*****************----------------------***************");

        boolean running = true;
        while (running) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("Currently logged in as: " + currentUserId);
            System.out.println("1.  Add Product to Catalog");
            System.out.println("2.  List All Products");
            System.out.println("3.  Add Product to Cart");
            System.out.println("4.  Remove Product from Cart");
            System.out.println("5.  View My Cart");
            System.out.println(
                    "6.  Apply Coupon Code (Active: " + (activeCouponCode == null ? "None" : activeCouponCode) + ")");
            System.out.println("7.  Place Order (Checkout)");
            System.out.println("8.  Cancel an Order");
            System.out.println("9.  View Order History");
            System.out.println("10. Check Low Stock Alerts");
            System.out.println("11. Return an Item");
            System.out.println("12. Run Concurrency Test (Stock Locks)");
            System.out.println("13. View System Audit Logs");
            System.out.println("14. Toggle Fault Injection (Failure Mode: "
                    + (FailureInjector.isInjectionEnabled() ? "ON" : "OFF") + ")");
            System.out.println("15. Switch to Another User");
            System.out.println("0.  Exit");
            System.out.print("Option > ");

            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1":
                        System.out.print("Enter Product ID: ");
                        String id = scanner.nextLine();
                        System.out.print("Enter Product Name: ");
                        String name = scanner.nextLine();
                        System.out.print("Enter Price: ");
                        double price = Double.parseDouble(scanner.nextLine());
                        System.out.print("Enter Stock: ");
                        int stock = Integer.parseInt(scanner.nextLine());
                        productService.addProduct(id, name, price, stock);
                        break;
                    case "2":
                        for (Product p : productService.getAllProducts()) {
                            System.out.println(p);
                        }
                        break;
                    case "3":
                        System.out.print("Enter Product ID: ");
                        String pId = scanner.nextLine();
                        System.out.print("Enter Qty: ");
                        int qty = Integer.parseInt(scanner.nextLine());
                        cartService.addToCart(currentUserId, pId, qty);
                        break;
                    case "4":
                        System.out.print("Enter Product ID: ");
                        String rId = scanner.nextLine();
                        System.out.print("Enter Qty: ");
                        int rQty = Integer.parseInt(scanner.nextLine());
                        cartService.removeFromCart(currentUserId, rId, rQty);
                        break;
                    case "5":
                        Cart c = cartService.getCart(currentUserId);
                        c.getItems().forEach((k, v) -> System.out.println("ID: " + k + " | Qty: " + v));
                        if (c.getItems().isEmpty())
                            System.out.println("Cart is currently empty.");
                        break;
                    case "6":
                        System.out.print("Enter coupon code: ");
                        activeCouponCode = scanner.nextLine();
                        System.out.println("Coupon applied.");
                        break;
                    case "7":
                        orderService.placeOrder(currentUserId, activeCouponCode);
                        activeCouponCode = null;
                        break;
                    case "8":
                        System.out.print("Enter Order ID to cancel: ");
                        String cancelId = scanner.nextLine();
                        orderService.cancelOrder(cancelId);
                        break;
                    case "9":
                        for (Order o : orderRepo.getAllOrders()) {
                            System.out.println(o);
                        }
                        break;
                    case "10":
                        productService.printLowStockAlerts();
                        break;
                    case "11":
                        System.out.print("Enter Order ID: ");
                        String retOrderId = scanner.nextLine();
                        System.out.print("Enter Product ID: ");
                        String retProdId = scanner.nextLine();
                        System.out.print("Enter Qty: ");
                        int retQty = Integer.parseInt(scanner.nextLine());
                        orderService.processReturn(retOrderId, retProdId, retQty);
                        break;
                    case "12":
                        runConcurrencyTest();
                        break;
                    case "13":
                        System.out.println("--- System Audit Logs ---");
                        try {
                            Files.lines(Paths.get("audit.log")).forEach(System.out::println);
                        } catch (Exception e) {
                            System.out.println("Log file not found or empty.");
                        }
                        break;
                    case "14":
                        FailureInjector.toggle();
                        break;
                    case "15":
                        System.out.print("Enter User ID: ");
                        currentUserId = scanner.nextLine();
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Input error: " + e.getMessage());
            }
        }
        scanner.close();
        System.exit(0);
    }

    private static void runConcurrencyTest() {
        System.out.println("\n--- STARTING CONCURRENCY STRESS TEST ---");
        productService.addProduct("STRESS_TEST", "Rare Collectible", 999.0, 1);

        Runnable userTask1 = () -> cartService.addToCart("User_A", "STRESS_TEST", 1);
        Runnable userTask2 = () -> cartService.addToCart("User_B", "STRESS_TEST", 1);

        Thread t1 = new Thread(userTask1);
        Thread t2 = new Thread(userTask2);

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {

        }
        System.out
                .println("Final Stock (Should be 0): " + productService.getProduct("STRESS_TEST").getAvailableStock());
    }
}
