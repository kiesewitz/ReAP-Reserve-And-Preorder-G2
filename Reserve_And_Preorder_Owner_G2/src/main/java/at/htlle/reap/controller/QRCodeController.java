package at.htlle.reap.controller;

import at.htlle.reap.model.Reservation;
import at.htlle.reap.service.QRCodeService;
import at.htlle.reap.service.ResService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/qr")
public class QRCodeController {

    private final QRCodeService qrCodeService;
    private final ResService resService;

    @Autowired
    public QRCodeController(QRCodeService qrCodeService, ResService resService) {
        this.qrCodeService = qrCodeService;
        this.resService = resService;
    }

    /**
     * Get QR code for a reservation
     * Returns the QR code as Base64 image and the check-in URL
     */
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<QRCodeService.QRCodeData> getReservationQRCode(
            @PathVariable Long reservationId,
            @RequestParam(required = false) Long guestId) {

        try {
            // Verify reservation exists
            Reservation reservation = resService.getReservationById(reservationId);

            // Generate QR code
            String baseUrl = "http://localhost:8083";
            QRCodeService.QRCodeData qrData = qrCodeService.generateCheckinQRCode(
                    baseUrl, reservationId, guestId
            );

            return ResponseEntity.ok(qrData);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Validate a QR code token
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateQRCode(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
        }

        QRCodeService.ValidationResult result = qrCodeService.validateToken(token);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.isValid());
        response.put("message", result.getMessage());

        if (result.isValid()) {
            response.put("reservationId", result.getReservationId());
            response.put("guestId", result.getGuestId());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Check-in via QR code scan
     * This endpoint is called when a waiter scans a QR code
     */
    @PostMapping("/checkin")
    public ResponseEntity<Map<String, Object>> checkinViaQR(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
        }

        // Validate token
        QRCodeService.ValidationResult validation = qrCodeService.validateToken(token);

        if (!validation.isValid()) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Invalid token",
                    "message", validation.getMessage()
            ));
        }

        try {
            // Check-in the reservation
            Reservation reservation = resService.checkIn(validation.getReservationId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reservation", reservation);
            response.put("message", "Check-in successful");
            response.put("tableNumber", reservation.getTableId());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Check-in failed",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Check-in via GET request (when QR code URL is opened in browser)
     * Example: http://localhost:8083/api/qr/checkin?token=xyz
     */
    @GetMapping("/checkin")
    public ResponseEntity<String> checkinViaUrl(@RequestParam String token) {
        // Validate token
        QRCodeService.ValidationResult validation = qrCodeService.validateToken(token);

        if (!validation.isValid()) {
            return ResponseEntity.status(403).body(
                    "<html><body><h1>Check-In fehlgeschlagen</h1><p>" +
                    validation.getMessage() + "</p></body></html>"
            );
        }

        try {
            // Check-in the reservation
            Reservation reservation = resService.checkIn(validation.getReservationId());

            return ResponseEntity.ok(
                    "<html><body style='text-align: center; font-family: Arial;'>" +
                    "<h1 style='color: green;'>✓ Check-In erfolgreich!</h1>" +
                    "<p><strong>Reservierungs-ID:</strong> " + reservation.getId() + "</p>" +
                    "<p><strong>Tisch:</strong> " + (reservation.getTableId() != null ? reservation.getTableId() : "Wird zugewiesen") + "</p>" +
                    "<p><strong>Gäste:</strong> " + reservation.getNumberOfGuests() + "</p>" +
                    "<p style='color: gray; margin-top: 30px;'>Sie können dieses Fenster jetzt schließen.</p>" +
                    "</body></html>"
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    "<html><body style='text-align: center; font-family: Arial;'>" +
                    "<h1 style='color: red;'>✗ Check-In fehlgeschlagen</h1>" +
                    "<p>" + e.getMessage() + "</p>" +
                    "</body></html>"
            );
        }
    }
}
