package at.htlle.reserveAndPreorderCookG2.controller;


import at.htlle.reserveAndPreorderCookG2.service.OrderService;
import at.htlle.reserveandpreordercookg2.model.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*") // erlaubt Zugriff von JS-Frontend
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // GET /api/orders
    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    // POST /api/orders
    @PostMapping
    public ResponseEntity<Order> addOrder(@RequestBody Order order) {
        orderService.addOrder(order);
        return ResponseEntity.ok(order);
    }

    // POST /api/orders/{id}/done
    @PostMapping("/{id}/done")
    public ResponseEntity<String> markAsDone(@PathVariable String id) {
        boolean success = orderService.markAsDone(id);
        if (success) {
            return ResponseEntity.ok("Order " + id + " marked as done.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
