package com.ecommerce.system;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class AuditLogger {
    private static final String LOG_FILE = "audit.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void log(String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = String.format("[%s] %s", timestamp, message);
        System.out.println("  [AUDIT] " + logEntry);
        
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println(logEntry);
        } catch (IOException e) {
            System.err.println("Could not write to audit log: " + e.getMessage());
        }
    }
}
