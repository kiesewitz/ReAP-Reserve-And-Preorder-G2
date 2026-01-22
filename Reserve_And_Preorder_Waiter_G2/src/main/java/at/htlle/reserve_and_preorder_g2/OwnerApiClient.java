package at.htlle.reserve_and_preorder_g2;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;

/**
 * Client to communicate with Owner App API (port 8083)
 */
@Service
public class OwnerApiClient {

    private final RestTemplate restTemplate;
    private final String OWNER_API_BASE = "http://localhost:8083/api";

    public OwnerApiClient() {
        this.restTemplate = new RestTemplate();
    }

    // ============ Reservations API ============

    public List<ReservationDto> getAllReservations() {
        String url = OWNER_API_BASE + "/reservations";
        ResponseEntity<List<ReservationDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ReservationDto>>() {}
        );
        return response.getBody();
    }

    public List<ReservationDto> getActiveReservations() {
        String url = OWNER_API_BASE + "/reservations/status/CHECKED_IN";
        ResponseEntity<List<ReservationDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ReservationDto>>() {}
        );
        return response.getBody();
    }

    public ReservationDto getReservation(Long id) {
        String url = OWNER_API_BASE + "/reservations/" + id;
        return restTemplate.getForObject(url, ReservationDto.class);
    }

    public ReservationDto checkIn(Long reservationId) {
        try {
            String url = OWNER_API_BASE + "/reservations/" + reservationId + "/checkin";
            System.out.println("Checking in reservation " + reservationId + " via: " + url);
            ReservationDto result = restTemplate.postForObject(url, null, ReservationDto.class);
            System.out.println("Check-in successful for reservation " + reservationId);
            return result;
        } catch (Exception e) {
            System.err.println("Check-in failed for reservation " + reservationId + ": " + e.getMessage());
            throw e;
        }
    }

    public ReservationDto completeReservation(Long reservationId) {
        String url = OWNER_API_BASE + "/reservations/" + reservationId + "/complete";
        return restTemplate.postForObject(url, null, ReservationDto.class);
    }

    public ReservationDto createWalkIn(Long tableId, int numberOfGuests) {
        String url = OWNER_API_BASE + "/reservations/walkin";
        WalkInRequest request = new WalkInRequest();
        request.tableId = tableId;
        request.numberOfGuests = numberOfGuests;
        return restTemplate.postForObject(url, request, ReservationDto.class);
    }

    // ============ Tables API ============

    public List<TableDto> getAllTables() {
        try {
            String url = OWNER_API_BASE + "/tables";
            System.out.println("Fetching tables from: " + url);
            ResponseEntity<List<TableDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<TableDto>>() {}
            );
            List<TableDto> tables = response.getBody();
            System.out.println("Fetched " + (tables != null ? tables.size() : 0) + " tables from Owner API");
            return tables != null ? tables : new java.util.ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error fetching tables from Owner API: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    public TableDto getTable(Long id) {
        String url = OWNER_API_BASE + "/tables/" + id;
        return restTemplate.getForObject(url, TableDto.class);
    }

    /**
     * Mark table for cleaning (Abservieren)
     */
    public TableDto clearTable(Long tableId) {
        try {
            String url = OWNER_API_BASE + "/tables/" + tableId + "/clear";
            return restTemplate.postForObject(url, null, TableDto.class);
        } catch (Exception e) {
            System.err.println("Error clearing table: " + e.getMessage());
            return null;
        }
    }

    /**
     * Mark table as available
     */
    public TableDto markTableAvailable(Long tableId) {
        try {
            String url = OWNER_API_BASE + "/tables/" + tableId + "/available";
            return restTemplate.postForObject(url, null, TableDto.class);
        } catch (Exception e) {
            System.err.println("Error marking table available: " + e.getMessage());
            return null;
        }
    }

    // ============ Payments API ============

    public PaymentDto processCashPayment(Long reservationId, double amount) {
        String url = OWNER_API_BASE + "/payments/cash";
        CashPaymentRequest request = new CashPaymentRequest();
        request.reservationId = reservationId;
        request.amount = amount;
        return restTemplate.postForObject(url, request, PaymentDto.class);
    }

    public PaymentDto processCreditCardPayment(Long reservationId, double amount, String token) {
        String url = OWNER_API_BASE + "/payments/credit-card";
        OnlinePaymentRequest request = new OnlinePaymentRequest();
        request.reservationId = reservationId;
        request.amount = amount;
        request.token = token;
        return restTemplate.postForObject(url, request, PaymentDto.class);
    }

    // DTOs for API communication
    public static class ReservationDto {
        public Long id;
        public Long customerId;
        public Long restaurantId;
        public Long tableId;
        public String reservationDateTime;
        public int durationMinutes;
        public int numberOfGuests;
        public String status;
        public boolean isGroupReservation;
        public String qrCode;
        public double cancellationFee;
        public String checkedInAt;
        public String createdAt;
        public String updatedAt;
    }

    public static class TableDto {
        public Long id;
        public Long restaurantId;
        public String tableNumber;
        public int capacity;
        public String status;
        public Long currentReservationId;
    }

    public static class PaymentDto {
        public Long id;
        public Long reservationId;
        public double amount;
        public String paymentMethod;
        public String paymentStatus;
        public String transactionId;
        public String paidAt;
    }

    public static class CashPaymentRequest {
        public Long reservationId;
        public double amount;
    }

    public static class OnlinePaymentRequest {
        public Long reservationId;
        public double amount;
        public String token;
    }

    public static class WalkInRequest {
        public Long tableId;
        public int numberOfGuests;
    }
}
