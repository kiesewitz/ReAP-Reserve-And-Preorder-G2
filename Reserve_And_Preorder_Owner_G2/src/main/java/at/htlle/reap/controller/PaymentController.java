package at.htlle.reap.controller;

import at.htlle.reap.enums.PaymentStatus;
import at.htlle.reap.model.Payment;
import at.htlle.reap.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Process cash payment (REAL - waiter marks as paid)
     * Body: { "reservationId": 1, "amount": 45.50 }
     */
    @PostMapping("/cash")
    public ResponseEntity<Payment> processCashPayment(@RequestBody CashPaymentRequest request) {
        try {
            Payment payment = paymentService.processCashPayment(
                    request.getReservationId(),
                    request.getAmount()
            );
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            System.err.println("Cash payment error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Process mock credit card payment (DEMO)
     * Body: { "reservationId": 1, "amount": 45.50, "cardToken": "tok_visa" }
     */
    @PostMapping("/credit-card")
    public ResponseEntity<Payment> processCreditCardPayment(@RequestBody OnlinePaymentRequest request) {
        try {
            Payment payment = paymentService.processMockCreditCardPayment(
                    request.getReservationId(),
                    request.getAmount(),
                    request.getToken()
            );
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            System.err.println("Credit card payment error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Process mock PayPal payment (DEMO)
     * Body: { "reservationId": 1, "amount": 45.50, "token": "user@example.com" }
     */
    @PostMapping("/paypal")
    public ResponseEntity<Payment> processPayPalPayment(@RequestBody OnlinePaymentRequest request) {
        try {
            Payment payment = paymentService.processMockPayPalPayment(
                    request.getReservationId(),
                    request.getAmount(),
                    request.getToken()
            );
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            System.err.println("PayPal payment error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get payment by reservation ID
     */
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<Payment> getPaymentByReservation(@PathVariable Long reservationId) {
        try {
            Payment payment = paymentService.getPaymentByReservationId(reservationId);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long paymentId) {
        try {
            Payment payment = paymentService.getPaymentById(paymentId);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all payments
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     * Get payments by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status));
    }

    /**
     * Check if reservation is paid
     */
    @GetMapping("/reservation/{reservationId}/status")
    public ResponseEntity<Map<String, Boolean>> checkPaymentStatus(@PathVariable Long reservationId) {
        boolean isPaid = paymentService.isReservationPaid(reservationId);
        return ResponseEntity.ok(Map.of("isPaid", isPaid));
    }

    /**
     * Refund a payment
     * Body: { "paymentId": 1, "refundAmount": 20.00 }
     */
    @PostMapping("/refund")
    public ResponseEntity<Payment> refundPayment(@RequestBody RefundRequest request) {
        try {
            Payment payment = paymentService.refundPayment(
                    request.getPaymentId(),
                    request.getRefundAmount()
            );
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            System.err.println("Refund error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Request DTOs
    public static class CashPaymentRequest {
        private Long reservationId;
        private BigDecimal amount;

        public Long getReservationId() {
            return reservationId;
        }

        public void setReservationId(Long reservationId) {
            this.reservationId = reservationId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class OnlinePaymentRequest {
        private Long reservationId;
        private BigDecimal amount;
        private String token;

        public Long getReservationId() {
            return reservationId;
        }

        public void setReservationId(Long reservationId) {
            this.reservationId = reservationId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class RefundRequest {
        private Long paymentId;
        private BigDecimal refundAmount;

        public Long getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(Long paymentId) {
            this.paymentId = paymentId;
        }

        public BigDecimal getRefundAmount() {
            return refundAmount;
        }

        public void setRefundAmount(BigDecimal refundAmount) {
            this.refundAmount = refundAmount;
        }
    }
}
