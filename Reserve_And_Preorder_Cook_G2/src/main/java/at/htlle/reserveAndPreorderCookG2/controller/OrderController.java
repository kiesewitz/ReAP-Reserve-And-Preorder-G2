package at.htlle.reserveAndPreorderCookG2.controller;

import at.htlle.reserveAndPreorderCookG2.dto.CreateOrderRequest;
import at.htlle.reserveAndPreorderCookG2.service.OrderService;
import at.htlle.reserveAndPreorderCookG2.model.Order;
import at.htlle.reserveAndPreorderCookG2.model.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Get all orders
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * Get active orders (PENDING and IN_KITCHEN only)
     */
    @GetMapping("/active")
    public ResponseEntity<List<Order>> getActiveOrders() {
        return ResponseEntity.ok(orderService.getActiveOrders());
    }

    /**
     * Get orders for waiter (PENDING, IN_KITCHEN, and READY)
     */
    @GetMapping("/waiter")
    public ResponseEntity<List<Order>> getWaiterOrders() {
        return ResponseEntity.ok(orderService.getWaiterOrders());
    }

    /**
     * Get orders by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    /**
     * Get single order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        try {
            Order order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create new order
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        // Create order from request
        Order order = new Order();
        order.setTableNumber(String.valueOf(request.getTableId()));
        order.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");

        // Set totalPrice from request or default to ZERO
        if (request.getTotalPrice() != null) {
            order.setTotalPrice(BigDecimal.valueOf(request.getTotalPrice()));
        } else {
            order.setTotalPrice(BigDecimal.ZERO);
        }

        // Add items
        if (request.getItems() != null) {
            for (CreateOrderRequest.OrderItemDto itemDto : request.getItems()) {
                OrderItem item = new OrderItem(itemDto.getName(), itemDto.getQuantity());
                order.addItem(item);
            }
        }

        Order created = orderService.createOrder(order);
        return ResponseEntity.ok(created);
    }

    /**
     * Mark order as "IN_KITCHEN" (cook started working)
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Order> startOrder(@PathVariable Long id) {
        try {
            Order order = orderService.markAsInKitchen(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Mark order as "READY" (cook finished, ready for serving)
     */
    @PostMapping("/{id}/ready")
    public ResponseEntity<Order> markAsReady(@PathVariable Long id) {
        try {
            Order order = orderService.markAsReady(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Mark order as "SERVED" (waiter delivered)
     */
    @PostMapping("/{id}/served")
    public ResponseEntity<Order> markAsServed(@PathVariable Long id) {
        try {
            Order order = orderService.markAsServed(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cancel order
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id) {
        try {
            Order order = orderService.cancelOrder(id);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Legacy endpoint - mark as done (maps to ready)
     */
    @PostMapping("/{id}/done")
    public ResponseEntity<String> markAsDone(@PathVariable String id) {
        boolean success = orderService.markAsDone(id);
        if (success) {
            return ResponseEntity.ok("Order " + id + " marked as done.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Toggle demo mode
     */
    @PostMapping("/demo-mode")
    public ResponseEntity<String> setDemoMode(@RequestParam boolean enabled) {
        orderService.setDemoMode(enabled);
        return ResponseEntity.ok("Demo mode set to: " + enabled);
    }
}
