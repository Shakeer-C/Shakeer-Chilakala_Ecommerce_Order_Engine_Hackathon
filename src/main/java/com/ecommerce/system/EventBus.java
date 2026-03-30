package com.ecommerce.system;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Task 14: Event-Driven System
public class EventBus {
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final Thread worker;
    
    public enum EventType {
        ORDER_CREATED,
        PAYMENT_SUCCESS,
        INVENTORY_UPDATED
    }

    public static class Event {
        public EventType type;
        public String payload;
        
        public Event(EventType type, String payload) {
            this.type = type;
            this.payload = payload;
        }
    }

    public EventBus() {
        worker = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Event event = eventQueue.take();
                    processEvent(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        worker.setDaemon(true); // Let app exit without waiting for this thread
        worker.start();
    }

    public void publish(EventType type, String payload) {
        eventQueue.offer(new Event(type, payload));
    }

    private void processEvent(Event event) {
        // Simulate processing sequentially
        try {
            Thread.sleep(500); // realistic delay for asynchronous processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        AuditLogger.log("Event Processed -> " + event.type + ": " + event.payload);
        
        if (FailureInjector.shouldFail("Event Processing")) {
            AuditLogger.log("Event processing aborted due to injected failure!");
        }
    }
}
