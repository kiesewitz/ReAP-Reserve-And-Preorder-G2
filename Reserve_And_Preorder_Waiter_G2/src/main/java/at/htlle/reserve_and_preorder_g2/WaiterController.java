package at.htlle.reserve_and_preorder_g2;

import at.htlle.reserve_and_preorder_g2.WaiterModels.WaiterStateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class WaiterController {

    private final WaiterService service;

    @Autowired
    public WaiterController(WaiterService service) {
        this.service = service;
    }

    // ============ WAITER STATE ============

    /**
     * Get waiter dashboard state (tables + orders + reservations)
     */
    @GetMapping("/waiter/state")
    public ResponseEntity<WaiterStateDto> getState() {
        return ResponseEntity.ok(service.getState());
    }

    // ============ RESERVATION MANAGEMENT ============

    /**
     * Get all active reservations (CHECKED_IN status)
     */
    @GetMapping("/reservations/active")
    public ResponseEntity<List<OwnerApiClient.ReservationDto>> getActiveReservations() {
        return ResponseEntity.ok(service.getActiveReservations());
    }

    /**
     * Check-in reservation via QR code
     */
    @PostMapping("/reservations/{id}/checkin")
    public ResponseEntity<OwnerApiClient.ReservationDto> checkInReservation(@PathVariable Long id) {
        try {
            OwnerApiClient.ReservationDto reservation = service.checkInReservation(id);
            return ResponseEntity.ok(reservation);
        } catch (Exception e) {
            System.err.println("Check-in error for reservation " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Complete reservation (end visit)
     */
    @PostMapping("/reservations/{id}/complete")
    public ResponseEntity<OwnerApiClient.ReservationDto> completeReservation(@PathVariable Long id) {
        try {
            OwnerApiClient.ReservationDto reservation = service.completeReservation(id);
            return ResponseEntity.ok(reservation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create walk-in (customer without reservation)
     * Body: { "tableId": 5, "numberOfGuests": 4 }
     */
    @PostMapping("/reservations/walkin")
    public ResponseEntity<OwnerApiClient.ReservationDto> createWalkIn(@RequestBody Map<String, Object> request) {
        try {
            Long tableId = Long.valueOf(request.get("tableId").toString());
            int numberOfGuests = Integer.parseInt(request.get("numberOfGuests").toString());

            OwnerApiClient.ReservationDto walkIn = service.createWalkIn(tableId, numberOfGuests);
            return ResponseEntity.ok(walkIn);
        } catch (Exception e) {
            System.err.println("Walk-in creation error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ============ PAYMENT MANAGEMENT ============

    /**
     * Process cash payment
     * Body: { "reservationId": 123, "amount": 45.50 }
     */
    @PostMapping("/payments/cash")
    public ResponseEntity<OwnerApiClient.PaymentDto> processCashPayment(
            @RequestBody Map<String, Object> request) {
        try {
            Long reservationId = Long.valueOf(request.get("reservationId").toString());
            double amount = Double.parseDouble(request.get("amount").toString());

            OwnerApiClient.PaymentDto payment = service.processCashPayment(reservationId, amount);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            System.err.println("Cash payment error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Process credit card payment
     * Body: { "reservationId": 123, "amount": 45.50, "token": "tok_visa" }
     */
    @PostMapping("/payments/credit-card")
    public ResponseEntity<OwnerApiClient.PaymentDto> processCardPayment(
            @RequestBody Map<String, Object> request) {
        try {
            Long reservationId = Long.valueOf(request.get("reservationId").toString());
            double amount = Double.parseDouble(request.get("amount").toString());
            String token = request.get("token") != null ? request.get("token").toString() : "tok_waiter";

            OwnerApiClient.PaymentDto payment = service.processCardPayment(reservationId, amount, token);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            System.err.println("Card payment error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ============ TABLE MANAGEMENT ============

    /**
     * Get all tables
     */
    @GetMapping("/tables")
    public ResponseEntity<List<OwnerApiClient.TableDto>> getAllTables() {
        return ResponseEntity.ok(service.getAllTables());
    }

    /**
     * Clear table (mark for cleaning)
     */
    @PostMapping("/tables/{tableId}/clear")
    public ResponseEntity<Void> clearTable(@PathVariable Long tableId) {
        service.clearTable(tableId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Finish table (mark as available)
     */
    @PostMapping("/tables/{tableId}/finish")
    public ResponseEntity<Void> finishTable(@PathVariable Long tableId) {
        boolean success = service.finishTable(tableId);
        if (success) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(409).build(); // Conflict
        }
    }

    // ============ ORDER MANAGEMENT (LOCAL DEMO) ============

    /**
     * Create new order
     */
    @PostMapping("/orders")
    public ResponseEntity<CookApiClient.CookOrderDto> createOrder(@RequestBody Map<String, Object> request) {
        Long tableId = Long.valueOf(request.get("tableId").toString());
        Long reservationId = null;
        if (request.get("reservationId") != null) {
            reservationId = Long.valueOf(request.get("reservationId").toString());
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) request.get("items");

        // Parse price
        Double totalPrice = null;
        if (request.get("totalPrice") != null) {
            totalPrice = Double.valueOf(request.get("totalPrice").toString());
        }

        List<CookApiClient.OrderItemDto> items = new ArrayList<>();
        for (Map<String, Object> itemMap : itemsList) {
            String name = itemMap.get("name").toString();
            int qty = Integer.parseInt(itemMap.get("qty").toString());
            Long menuItemId = itemMap.get("menuItemId") != null ? Long.valueOf(itemMap.get("menuItemId").toString()) : null;
            Double unitPrice = itemMap.get("unitPrice") != null ? Double.valueOf(itemMap.get("unitPrice").toString()) : null;
            String specialInstructions = itemMap.get("specialInstructions") != null ? itemMap.get("specialInstructions").toString() : null;
            items.add(new CookApiClient.OrderItemDto(name, qty, menuItemId, unitPrice, specialInstructions));
        }

        CookApiClient.CookOrderDto order = service.createOrder(tableId, reservationId, items, totalPrice);
        if (order != null) {
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Mark order as served
     */
    @PostMapping("/orders/{orderId}/served")
    public ResponseEntity<Void> markOrderServed(@PathVariable Long orderId) {
        service.markOrderServed(orderId);
        return ResponseEntity.noContent().build();
    }
}
