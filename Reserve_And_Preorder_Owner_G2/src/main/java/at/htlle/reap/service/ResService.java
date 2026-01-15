package at.htlle.reap.service;

import at.htlle.reap.enums.ReservationStatus;
import at.htlle.reap.enums.TableStatus;
import at.htlle.reap.model.GroupMember;
import at.htlle.reap.model.Reservation;
import at.htlle.reap.model.Table;
import at.htlle.reap.repository.ReservationRepository;
import at.htlle.reap.repository.TableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResService {

    private final ReservationRepository reservationRepository;
    private final TableRepository tableRepository;
    private final QRCodeService qrCodeService;

    // Base URL for QR codes (in production, load from configuration)
    private static final String BASE_URL = "http://localhost:8083";

    @Autowired
    public ResService(ReservationRepository reservationRepository,
                     TableRepository tableRepository,
                     QRCodeService qrCodeService) {
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
        this.qrCodeService = qrCodeService;
    }

    /**
     * Get all reservations
     */
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    /**
     * Get reservation by ID
     */
    public Reservation getReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found with id: " + id));
    }

    /**
     * Get reservations by customer ID
     */
    public List<Reservation> getReservationsByCustomerId(Long customerId) {
        return reservationRepository.findByCustomerId(customerId);
    }

    /**
     * Get reservations by restaurant ID
     */
    public List<Reservation> getReservationsByRestaurantId(Long restaurantId) {
        return reservationRepository.findByRestaurantId(restaurantId);
    }

    /**
     * Create a new reservation
     */
    @Transactional
    public Reservation createReservation(Reservation reservation) {
        // Set initial status
        reservation.setStatus(ReservationStatus.PENDING);

        // Find available table
        List<Table> availableTables = tableRepository.findAvailableTablesWithCapacity(
                reservation.getRestaurantId(),
                reservation.getNumberOfGuests()
        );

        if (!availableTables.isEmpty()) {
            Table table = availableTables.get(0);
            reservation.setTableId(table.getId());

            // Reserve the table
            table.setStatus(TableStatus.RESERVED);
            table.setCurrentReservationId(reservation.getId());
            tableRepository.save(table);

            // Update status to CONFIRMED
            reservation.setStatus(ReservationStatus.CONFIRMED);
        }

        // Save reservation first to get ID
        Reservation saved = reservationRepository.save(reservation);

        // Generate QR code for check-in
        String qrToken = qrCodeService.generateCheckinToken(saved.getId(), null);
        saved.setQrCode(qrToken);
        saved = reservationRepository.save(saved);

        System.out.println("Created reservation: " + saved + " with QR token");

        return saved;
    }

    /**
     * Update reservation table assignment
     */
    @Transactional
    public void updateReservationTable(Long reservationId, Long tableId) {
        Reservation reservation = getReservationById(reservationId);
        reservation.setTableId(tableId);
        
        // If status is PENDING, update to CONFIRMED
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
        }
        
        reservationRepository.save(reservation);
        System.out.println("Updated reservation " + reservationId + " with table " + tableId);
    }

    /**
     * Create reservation with group members
     */
    @Transactional
    public Reservation createGroupReservation(Reservation reservation, List<String> guestEmails) {
        reservation.setGroupReservation(true);

        // Create reservation first
        Reservation saved = createReservation(reservation);

        // Add group members with individual QR codes
        long guestIdCounter = 1;
        for (String email : guestEmails) {
            GroupMember member = new GroupMember(email, email);

            // Generate individual QR code for this group member
            String memberQrToken = qrCodeService.generateCheckinToken(saved.getId(), guestIdCounter);
            member.setQrCode(memberQrToken);

            saved.addGroupMember(member);
            guestIdCounter++;
        }

        return reservationRepository.save(saved);
    }

    /**
     * Cancel reservation with fee calculation
     * Business Rule: Free cancellation until 30 min before, then 10€ (single) or 20€ (group)
     */
    @Transactional
    public Reservation cancelReservation(Long id, LocalDateTime cancelTime) {
        Reservation reservation = getReservationById(id);

        if (reservation.getStatus() == ReservationStatus.CANCELLED ||
            reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel reservation with status: " + reservation.getStatus());
        }

        // Calculate cancellation fee
        BigDecimal fee = calculateCancellationFee(reservation, cancelTime);
        reservation.setCancellationFee(fee);
        reservation.setStatus(ReservationStatus.CANCELLED);

        // Free up table
        if (reservation.getTableId() != null) {
            Table table = tableRepository.findById(reservation.getTableId()).orElse(null);
            if (table != null) {
                table.setStatus(TableStatus.AVAILABLE);
                table.setCurrentReservationId(null);
                tableRepository.save(table);
            }
        }

        System.out.println("Cancelled reservation " + id + " with fee: " + fee + "€");
        return reservationRepository.save(reservation);
    }

    /**
     * Calculate cancellation fee based on time until reservation
     * Business Rule: Free if >= 30 min before, otherwise 10€ (single) or 20€ (group)
     */
    public BigDecimal calculateCancellationFee(Reservation reservation, LocalDateTime cancelTime) {
        long minutesUntilReservation = Duration.between(cancelTime, reservation.getReservationDateTime()).toMinutes();

        if (minutesUntilReservation >= 30) {
            return BigDecimal.ZERO;
        } else {
            return reservation.isGroupReservation() ? new BigDecimal("20.00") : new BigDecimal("10.00");
        }
    }

    /**
     * Check-in a reservation
     * Allows PENDING or CONFIRMED status
     */
    @Transactional
    public Reservation checkIn(Long reservationId) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED && 
            reservation.getStatus() != ReservationStatus.PENDING) {
            throw new RuntimeException("Cannot check-in reservation with status: " + reservation.getStatus());
        }

        // Check if table is assigned
        if (reservation.getTableId() == null) {
            throw new RuntimeException("Cannot check-in: No table assigned to reservation " + reservationId);
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setCheckedInAt(LocalDateTime.now());

        // Update table status and link reservation
        Table table = tableRepository.findById(reservation.getTableId())
            .orElseThrow(() -> new RuntimeException("Table not found: " + reservation.getTableId()));
        
        table.setStatus(TableStatus.OCCUPIED);
        table.setCurrentReservationId(reservationId);
        tableRepository.save(table);

        System.out.println("Checked in reservation: " + reservationId + " at table " + table.getTableNumber());
        return reservationRepository.save(reservation);
    }

    /**
     * Walk-in: Mark table as occupied without reservation
     * Creates a temporary walk-in reservation
     */
    @Transactional
    public Reservation createWalkIn(Long tableId, int numberOfGuests) {
        Table table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found: " + tableId));

        // Create walk-in reservation
        Reservation walkIn = new Reservation();
        walkIn.setTableId(tableId);
        walkIn.setRestaurantId(table.getRestaurantId());
        walkIn.setNumberOfGuests(numberOfGuests);
        walkIn.setReservationDateTime(LocalDateTime.now());
        walkIn.setStatus(ReservationStatus.CHECKED_IN);
        walkIn.setCheckedInAt(LocalDateTime.now());
        walkIn.setCustomerId(0L); // Virtual customer for walk-in
        
        Reservation saved = reservationRepository.save(walkIn);

        // Mark table as occupied
        table.setStatus(TableStatus.OCCUPIED);
        table.setCurrentReservationId(saved.getId());
        tableRepository.save(table);

        System.out.println("Created walk-in reservation: " + saved.getId() + " for table: " + tableId);
        return saved;
    }

    /**
     * Complete a reservation
     */
    @Transactional
    public Reservation completeReservation(Long reservationId) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != ReservationStatus.TIMEOUT_WARNING) {
            throw new RuntimeException("Cannot complete reservation with status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.COMPLETED);

        // Free up table
        if (reservation.getTableId() != null) {
            Table table = tableRepository.findById(reservation.getTableId()).orElse(null);
            if (table != null) {
                table.setStatus(TableStatus.CLEANING);
                table.setCurrentReservationId(null);
                tableRepository.save(table);
            }
        }

        System.out.println("Completed reservation: " + reservationId);
        return reservationRepository.save(reservation);
    }

    /**
     * Mark reservation as no-show and charge absence fee
     * Business Rule: 10€ per person
     */
    @Transactional
    public Reservation markAsNoShow(Long reservationId) {
        Reservation reservation = getReservationById(reservationId);

        reservation.setStatus(ReservationStatus.NO_SHOW);

        // Calculate absence fee: 10€ per guest
        BigDecimal absenceFee = new BigDecimal(reservation.getNumberOfGuests() * 10);
        reservation.setCancellationFee(absenceFee);

        // Free up table
        if (reservation.getTableId() != null) {
            Table table = tableRepository.findById(reservation.getTableId()).orElse(null);
            if (table != null) {
                table.setStatus(TableStatus.AVAILABLE);
                table.setCurrentReservationId(null);
                tableRepository.save(table);
            }
        }

        System.out.println("Marked reservation " + reservationId + " as NO_SHOW. Absence fee: " + absenceFee + "€");
        return reservationRepository.save(reservation);
    }

    /**
     * Find potential no-shows (CONFIRMED but 15+ min past reservation time without check-in)
     */
    public List<Reservation> findPotentialNoShows() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
        return reservationRepository.findPotentialNoShows(cutoffTime);
    }

    /**
     * Find timeout candidates (checked in for more than 2 hours)
     */
    public List<Reservation> findTimeoutCandidates() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(120);
        return reservationRepository.findTimeoutCandidates(cutoffTime);
    }

    /**
     * Mark reservation with timeout warning
     */
    @Transactional
    public Reservation markTimeoutWarning(Long reservationId) {
        Reservation reservation = getReservationById(reservationId);
        reservation.setStatus(ReservationStatus.TIMEOUT_WARNING);
        System.out.println("Marked reservation " + reservationId + " with TIMEOUT_WARNING");
        return reservationRepository.save(reservation);
    }

    /**
     * Get active reservations (currently checked in)
     */
    public List<Reservation> getActiveReservations() {
        return reservationRepository.findActiveReservations();
    }

    /**
     * Get reservations by status
     */
    public List<Reservation> getReservationsByStatus(String status) {
        try {
            ReservationStatus statusEnum = ReservationStatus.valueOf(status.toUpperCase());
            return reservationRepository.findByStatus(statusEnum);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid reservation status: " + status);
        }
    }

    /**
     * Delete reservation (admin only)
     */
    @Transactional
    public void deleteReservation(Long id) {
        Reservation reservation = getReservationById(id);

        // Free up table if assigned
        if (reservation.getTableId() != null) {
            Table table = tableRepository.findById(reservation.getTableId()).orElse(null);
            if (table != null) {
                table.setStatus(TableStatus.AVAILABLE);
                table.setCurrentReservationId(null);
                tableRepository.save(table);
            }
        }

        reservationRepository.deleteById(id);
        System.out.println("Deleted reservation: " + id);
    }
}
