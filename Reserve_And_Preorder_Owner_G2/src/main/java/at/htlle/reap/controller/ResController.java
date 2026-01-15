package at.htlle.reap.controller;

import at.htlle.reap.model.Reservation;
import at.htlle.reap.service.ResService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/reservations")
public class ResController {

    private final ResService resService;

    @Autowired
    public ResController(ResService resService) {
        this.resService = resService;
    }

    /**
     * Get all reservations
     */
    @GetMapping
    public ResponseEntity<List<Reservation>> getAllReservations() {
        return ResponseEntity.ok(resService.getAllReservations());
    }

    /**
     * Get reservation by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationById(@PathVariable Long id) {
        try {
            Reservation reservation = resService.getReservationById(id);
            return ResponseEntity.ok(reservation);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get reservations by customer ID
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Reservation>> getReservationsByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(resService.getReservationsByCustomerId(customerId));
    }

    /**
     * Get reservations by restaurant ID
     */
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<Reservation>> getReservationsByRestaurantId(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(resService.getReservationsByRestaurantId(restaurantId));
    }

    /**
     * Get active reservations
     */
    @GetMapping("/active")
    public ResponseEntity<List<Reservation>> getActiveReservations() {
        return ResponseEntity.ok(resService.getActiveReservations());
    }

    /**
     * Get reservations by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Reservation>> getReservationsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(resService.getReservationsByStatus(status));
    }

    /**
     * Create a new reservation
     * Body: { "customerId": 1, "restaurantId": 1, "reservationDateTime": "2026-01-20T19:00", "numberOfGuests": 2 }
     */
    @PostMapping
    public ResponseEntity<Reservation> createReservation(@RequestBody Reservation reservation) {
        try {
            Reservation created = resService.createReservation(reservation);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            System.err.println("Error creating reservation: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create a group reservation
     * Body: { "reservation": {...}, "guestEmails": ["a@x.de", "b@x.de"] }
     */
    @PostMapping("/group")
    public ResponseEntity<Reservation> createGroupReservation(
            @RequestBody GroupReservationRequest request) {
        try {
            Reservation created = resService.createGroupReservation(
                    request.getReservation(),
                    request.getGuestEmails()
            );
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            System.err.println("Error creating group reservation: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cancel a reservation
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Reservation> cancelReservation(@PathVariable Long id) {
        try {
            Reservation cancelled = resService.cancelReservation(id, LocalDateTime.now());
            return ResponseEntity.ok(cancelled);
        } catch (RuntimeException e) {
            System.err.println("Error cancelling reservation: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Check-in a reservation
     */
    @PostMapping("/{id}/checkin")
    public ResponseEntity<Reservation> checkIn(@PathVariable Long id) {
        try {
            Reservation checkedIn = resService.checkIn(id);
            return ResponseEntity.ok(checkedIn);
        } catch (RuntimeException e) {
            System.err.println("Error checking in reservation " + id + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Complete a reservation
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Reservation> completeReservation(@PathVariable Long id) {
        try {
            Reservation completed = resService.completeReservation(id);
            return ResponseEntity.ok(completed);
        } catch (RuntimeException e) {
            System.err.println("Error completing reservation: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create walk-in (customer without reservation)
     * Body: { "tableId": 5, "numberOfGuests": 4 }
     */
    @PostMapping("/walkin")
    public ResponseEntity<Reservation> createWalkIn(@RequestBody Map<String, Object> request) {
        try {
            Long tableId = Long.valueOf(request.get("tableId").toString());
            int numberOfGuests = Integer.parseInt(request.get("numberOfGuests").toString());
            
            Reservation walkIn = resService.createWalkIn(tableId, numberOfGuests);
            return ResponseEntity.ok(walkIn);
        } catch (RuntimeException e) {
            System.err.println("Error creating walk-in: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mark as no-show
     */
    @PostMapping("/{id}/no-show")
    public ResponseEntity<Reservation> markAsNoShow(@PathVariable Long id) {
        try {
            Reservation noShow = resService.markAsNoShow(id);
            return ResponseEntity.ok(noShow);
        } catch (RuntimeException e) {
            System.err.println("Error marking no-show: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get potential no-shows
     */
    @GetMapping("/no-shows")
    public ResponseEntity<List<Reservation>> getPotentialNoShows() {
        return ResponseEntity.ok(resService.findPotentialNoShows());
    }

    /**
     * Get timeout candidates
     */
    @GetMapping("/timeouts")
    public ResponseEntity<List<Reservation>> getTimeoutCandidates() {
        return ResponseEntity.ok(resService.findTimeoutCandidates());
    }

    /**
     * Inner class for group reservation request
     */
    public static class GroupReservationRequest {
        private Reservation reservation;
        private List<String> guestEmails;

        public Reservation getReservation() {
            return reservation;
        }

        public void setReservation(Reservation reservation) {
            this.reservation = reservation;
        }

        public List<String> getGuestEmails() {
            return guestEmails;
        }

        public void setGuestEmails(List<String> guestEmails) {
            this.guestEmails = guestEmails;
        }
    }
}
