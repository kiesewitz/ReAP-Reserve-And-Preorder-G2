package at.htlle.reap.repository;

import at.htlle.reap.enums.ReservationStatus;
import at.htlle.reap.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Find reservations by customer ID
    List<Reservation> findByCustomerId(Long customerId);

    // Find reservations by restaurant ID
    List<Reservation> findByRestaurantId(Long restaurantId);

    // Find reservations by status
    List<Reservation> findByStatus(ReservationStatus status);

    // Find reservations by restaurant and status
    List<Reservation> findByRestaurantIdAndStatus(Long restaurantId, ReservationStatus status);

    // Find reservations by date range
    @Query("SELECT r FROM Reservation r WHERE r.reservationDateTime BETWEEN :startDate AND :endDate")
    List<Reservation> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // Find potential no-shows: reservations that are CONFIRMED but past their time + 15 minutes
    @Query("SELECT r FROM Reservation r WHERE r.status = 'CONFIRMED' " +
           "AND r.reservationDateTime < :cutoffTime " +
           "AND r.checkedInAt IS NULL")
    List<Reservation> findPotentialNoShows(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Find timeout candidates: reservations checked in more than 2 hours ago
    @Query("SELECT r FROM Reservation r WHERE r.status = 'CHECKED_IN' " +
           "AND r.checkedInAt < :cutoffTime")
    List<Reservation> findTimeoutCandidates(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Find active reservations (checked in but not completed)
    @Query("SELECT r FROM Reservation r WHERE r.status = 'CHECKED_IN'")
    List<Reservation> findActiveReservations();

    // Check if table is available at specific time
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN false ELSE true END FROM Reservation r " +
           "WHERE r.tableId = :tableId " +
           "AND r.status IN ('CONFIRMED', 'CHECKED_IN') " +
           "AND r.reservationDateTime <= :endTime " +
           "AND TIMESTAMPADD(MINUTE, r.durationMinutes, r.reservationDateTime) >= :startTime")
    boolean isTableAvailable(@Param("tableId") Long tableId,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);
}
