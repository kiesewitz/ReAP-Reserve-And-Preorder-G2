package at.htlle.reap.repository;

import at.htlle.reap.enums.PaymentStatus;
import at.htlle.reap.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payment by reservation ID
    Optional<Payment> findByReservationId(Long reservationId);

    // Find all payments by status
    List<Payment> findByPaymentStatus(PaymentStatus status);

    // Find payments by transaction ID
    Optional<Payment> findByTransactionId(String transactionId);
}
