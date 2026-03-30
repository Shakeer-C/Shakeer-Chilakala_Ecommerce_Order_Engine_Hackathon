# Distributed E-Commerce Order Engine Backend

This project represents a fully functional simulated backend for an e-commerce platform, built purely in Java. It natively implements state-of-the-art enterprise patterns such as Event-Driven Architecture, Service/Microservice logic layer separation, Idempotency keys, and Logical Thread Locking to solve real-world problems like race-conditions, overselling, and failed transactions.

## Features Implemented
- **Concurrency & Locking System**: Safely handles multi-user cart collisions via `ReentrantLock` built into products to prevent overselling.
- **Microservices-style Separation**: `OrderService`, `PaymentService`, `CartService`, and `ProductService` decoupled.
- **Event-Driven Subsystem**: Handles downstream actions via a native `BlockingQueue`-backed `EventBus`.
- **Saga Pattern Rollback**: `OrderService` reverses previously claimed inventory natively upon simulated payment failure.
- **Cart Expiration**: A dedicated background thread automatically spins to evict shopping cart claims that surpass a configurable TTL.
- **Fraud Detection**: User throttle monitors ensure rapid or highly excessive purchasing is flagged to an immutable Audit Log file natively.
- **Failure Injector (Chaos Toolkit)**: Menu Option 14 can turn on randomized backend failure injection, allowing easy manual tests of the application's transaction rollback health during networking disruption.

## Design Approach
- **In-Memory Thread-Safe Repositories**: Using `ConcurrentHashMap` adapters ensures high-throughput fake DB layers with minimal boilerplate.
- **Stateless Domain Approach**: Pushing locks into the specific Data Nodes (such as `Product.java`) avoids deadlocking total service pipelines, reducing latency.

## Assumptions
- There's no true database connection configured; memory maps vanish upon JVM exit.
- `audit.log` will be generated locally within the present working directory.
- `EventBus` latency is simulated at 500ms processing delay natively.
- Payment Service simulates success on a generic 80/20 boolean probability roll.

## How to Compile & Run
This project uses strictly standard Java (built against Java 17). No external libraries are needed.

1. Ensure `javac` is available on your machine.
2. Open your terminal at the root of the project structure (`a:\VS-Project\Tw`).
3. **Compile everything**:
   ```bash
   javac -d out `find src -name "*.java"`
   ```
   *(For Windows Environments PowerShell)*:
   ```powershell
   javac -d out (Get-ChildItem -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
   ```
4. **Boot the application**:
   ```bash
   java -cp out com.ecommerce.CLIApplication
   ```
