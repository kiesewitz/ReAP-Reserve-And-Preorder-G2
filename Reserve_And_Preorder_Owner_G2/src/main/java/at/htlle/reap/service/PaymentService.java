package at.htlle.reap.service;

import at.htlle.reap.enums.PaymentMethod;
import at.htlle.reap.enums.PaymentStatus;
import at.htlle.reap.model.Payment;
import at.htlle.reap.model.Reservation;
import at.htlle.reap.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ResService resService;
    private final Random random = new Random();

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, ResService resService) {
        this.paymentRepository = paymentRepository;
        this.resService = resService;
    }

    /**
     * Process cash payment (REAL - handled by waiter)
     * This is the only real payment method in the system
     */
    @Transactional
    public Payment processCashPayment(Long reservationId, BigDecimal amount) {
        // Verify reservation exists
        Reservation reservation = resService.getReservationById(reservationId);

        // Check if payment already exists
        Payment existing = paymentRepository.findByReservationId(reservationId).orElse(null);
        if (existing != null && existing.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new RuntimeException("Payment already completed for this reservation");
        }

        // Create payment
        Payment payment = new Payment(reservationId, amount, PaymentMethod.CASH);
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionId("CASH_" + System.currentTimeMillis());

        Payment saved = paymentRepository.save(payment);
        System.out.println("Cash payment processed: " + saved);

        return saved;
    }

    /**
     * Process mock online payment (DEMO - Credit Card)
     * Simulates payment processing with 2-second delay
     */
    @Transactional
    public Payment processMockCreditCardPayment(Long reservationId, BigDecimal amount, String cardToken) {
        // Verify reservation exists
        Reservation reservation = resService.getReservationById(reservationId);

        // Check if payment already exists
        Payment existing = paymentRepository.findByReservationId(reservationId).orElse(null);
        if (existing != null && existing.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new RuntimeException("Payment already completed for this reservation");
        }

        // Create payment
        Payment payment = new Payment(reservationId, amount, PaymentMethod.CREDIT_CARD);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        try {
            // Simulate payment processing delay
            Thread.sleep(2000);

            // Simulate 5% failure rate for testing
            if (random.nextInt(100) < 5) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setTransactionId("FAILED_" + UUID.randomUUID());
                paymentRepository.save(payment);
                throw new RuntimeException("Payment declined by card issuer (DEMO)");
            }

            // Payment successful
            payment.setPaymentStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setTransactionId("CARD_" + UUID.randomUUID());

            Payment saved = paymentRepository.save(payment);
            System.out.println("Mock credit card payment processed: " + saved);

            return saved;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Payment processing interrupted");
        }
    }

    /**
     * Process mock online payment (DEMO - PayPal)
     * Simulates payment processing with 2-second delay
     */
    @Transactional
    public Payment processMockPayPalPayment(Long reservationId, BigDecimal amount, String paypalEmail) {
        // Verify reservation exists
        Reservation reservation = resService.getReservationById(reservationId);

        // Check if payment already exists
        Payment existing = paymentRepository.findByReservationId(reservationId).orElse(null);
        if (existing != null && existing.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new RuntimeException("Payment already completed for this reservation");
        }

        // Create payment
        Payment payment = new Payment(reservationId, amount, PaymentMethod.PAYPAL);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        try {
            // Simulate payment processing delay
            Thread.sleep(2000);

            // Simulate 5% failure rate for testing
            if (random.nextInt(100) < 5) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setTransactionId("FAILED_" + UUID.randomUUID());
                paymentRepository.save(payment);
                throw new RuntimeException("PayPal payment failed (DEMO)");
            }

            // Payment successful
            payment.setPaymentStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setTransactionId("PAYPAL_" + UUID.randomUUID());

            Payment saved = paymentRepository.save(payment);
            System.out.println("Mock PayPal payment processed: " + saved);

            return saved;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Payment processing interrupted");
        }
    }

    /**
     * Refund a payment
     */
    @Transactional
    public Payment refundPayment(Long paymentId, BigDecimal refundAmount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("Cannot refund payment with status: " + payment.getPaymentStatus());
        }

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new RuntimeException("Refund amount cannot exceed payment amount");
        }

        payment.setRefundAmount(refundAmount);
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        System.out.println("Refunded payment: " + saved);

        return saved;
    }

    /**
     * Get payment by reservation ID
     */
    public Payment getPaymentByReservationId(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new RuntimeException("No payment found for reservation: " + reservationId));
    }

    /**
     * Get payment by ID
     */
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));
    }

    /**
     * Get all payments
     */
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * Get payments by status
     */
    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByPaymentStatus(status);
    }

    /**
     * Check if reservation is paid
     */
    public boolean isReservationPaid(Long reservationId) {
        Payment payment = paymentRepository.findByReservationId(reservationId).orElse(null);
        return payment != null && payment.getPaymentStatus() == PaymentStatus.COMPLETED;
    }
}
