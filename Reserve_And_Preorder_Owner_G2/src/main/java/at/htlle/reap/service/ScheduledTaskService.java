package at.htlle.reap.service;

import at.htlle.reap.model.Reservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduledTaskService {

    private final ResService resService;

    @Autowired
    public ScheduledTaskService(ResService resService) {
        this.resService = resService;
    }

    /**
     * Check for no-shows every minute
     * Business Rule: Mark as NO_SHOW if reservation time + 15 minutes has passed without check-in
     * Charge: 10€ per person
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void checkNoShows() {
        List<Reservation> potentialNoShows = resService.findPotentialNoShows();

        if (!potentialNoShows.isEmpty()) {
            System.out.println("⚠️ Found " + potentialNoShows.size() + " potential no-shows");

            for (Reservation reservation : potentialNoShows) {
                try {
                    resService.markAsNoShow(reservation.getId());
                    System.out.println("Marked reservation " + reservation.getId() + " as NO_SHOW");

                    // TODO: Send email notification to customer
                    // TODO: Send notification to restaurant staff

                } catch (Exception e) {
                    System.err.println("Error marking no-show for reservation " + reservation.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Check for timeouts every minute
     * Business Rule: Warn if checked-in for more than 2 hours (120 minutes)
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void checkTimeouts() {
        List<Reservation> timeoutCandidates = resService.findTimeoutCandidates();

        if (!timeoutCandidates.isEmpty()) {
            System.out.println("⏰ Found " + timeoutCandidates.size() + " timeout candidates");

            for (Reservation reservation : timeoutCandidates) {
                try {
                    // Only mark if not already warned
                    if (reservation.getStatus().toString().equals("CHECKED_IN")) {
                        resService.markTimeoutWarning(reservation.getId());
                        System.out.println("Marked reservation " + reservation.getId() + " with TIMEOUT_WARNING");

                        // TODO: Send notification to waiter
                        // TODO: Send notification to customer
                    }

                } catch (Exception e) {
                    System.err.println("Error marking timeout for reservation " + reservation.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Automatic table cleanup check (runs every 5 minutes)
     * Sets tables from CLEANING to AVAILABLE after 15 minutes
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void checkTableCleanup() {
        // TODO: Implement when Table service has getTablesByStatus method
        // This would automatically set tables from CLEANING to AVAILABLE after a delay
        System.out.println("🧹 Table cleanup check (not yet implemented)");
    }

    /**
     * Daily summary report (runs at midnight)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void generateDailySummary() {
        System.out.println("📊 Generating daily summary report...");

        // TODO: Generate statistics
        // - Total reservations today
        // - No-shows count
        // - Revenue
        // - Average duration
        // - Send email to owner

        System.out.println("Daily summary report completed");
    }
}
