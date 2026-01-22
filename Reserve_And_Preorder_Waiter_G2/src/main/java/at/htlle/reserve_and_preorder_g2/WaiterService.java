package at.htlle.reserve_and_preorder_g2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static at.htlle.reserve_and_preorder_g2.WaiterModels.*;

/**
 * Waiter Service now uses Owner API for reservations and tables
 */
@Service
public class WaiterService {

    private final OwnerApiClient ownerApi;
    private final CookApiClient cookApi;

    @Autowired
    public WaiterService(OwnerApiClient ownerApi, CookApiClient cookApi) {
        this.ownerApi = ownerApi;
        this.cookApi = cookApi;
    }

    /**
     * Get current waiter state (tables + orders + reservations)
     */
    public WaiterStateDto getState() {
        System.out.println("Getting waiter state...");
        
        // Fetch tables from Owner API
        List<OwnerApiClient.TableDto> apiTables = ownerApi.getAllTables();
        System.out.println("Received " + apiTables.size() + " tables from Owner API");
        
        List<TableDto> tables = apiTables.stream()
                .map(this::convertToWaiterTable)
                .collect(Collectors.toList());
        System.out.println("Converted to " + tables.size() + " waiter tables");

        // Fetch ALL reservations to get currentReservationId for RESERVED tables
        List<OwnerApiClient.ReservationDto> allReservations = ownerApi.getAllReservations();
        System.out.println("Fetched " + allReservations.size() + " total reservations");

        // Update currentReservationId from reservations that have tableId assigned
        for (OwnerApiClient.ReservationDto res : allReservations) {
            if (res.tableId != null && ("PENDING".equals(res.status) || "CONFIRMED".equals(res.status) || "CHECKED_IN".equals(res.status))) {
                tables.stream()
                        .filter(t -> t.id.equals(res.tableId))
                        .findFirst()
                        .ifPresent(t -> {
                            // Update currentReservationId if table doesn't have one yet
                            if (t.currentReservationId == null) {
                                t.currentReservationId = res.id;
                                System.out.println("Set currentReservationId=" + res.id + " for table " + t.id + " from reservation");
                            }
                        });
            }
        }

        // Build reservationId -> tableId mapping (for preorders with TBD tableNumber)
        Map<Long, Long> reservationToTableId = new HashMap<>();
        for (OwnerApiClient.ReservationDto res : allReservations) {
            if (res.id != null && res.tableId != null) {
                reservationToTableId.put(res.id, res.tableId);
            }
        }

        // Fetch orders from Cook API
        List<CookApiClient.CookOrderDto> cookOrders = cookApi.getActiveOrders();
        List<OrderDto> orders = cookOrders.stream()
                .map(o -> convertToWaiterOrder(o, reservationToTableId))
                .collect(Collectors.toList());

        return new WaiterStateDto(tables, orders);
    }

    /**
     * Convert Owner API table to Waiter table DTO
     */
    private TableDto convertToWaiterTable(OwnerApiClient.TableDto apiTable) {
        TableDto table = new TableDto();
        table.id = apiTable.id;
        table.restaurantId = apiTable.restaurantId;
        table.name = "Tisch " + apiTable.tableNumber;
        table.capacity = apiTable.capacity;
        table.currentReservationId = apiTable.currentReservationId;

        // Debug log for RESERVED tables
        if ("RESERVED".equals(apiTable.status)) {
            System.out.println("Table " + apiTable.tableNumber + " is RESERVED with currentReservationId: " + apiTable.currentReservationId);
        }

        // Map table status from Owner API to Waiter status
        switch (apiTable.status) {
            case "AVAILABLE":
                table.status = TableStatus.LEER;
                break;
            case "RESERVED":
                table.status = TableStatus.RESERVIERT;
                break;
            case "OCCUPIED":
                table.status = TableStatus.BELEGT;
                break;
            case "CLEANING":
                table.status = TableStatus.ABSERVIEREN;
                break;
            default:
                table.status = TableStatus.LEER;
        }

        return table;
    }

    /**
     * Convert Cook API order to Waiter order DTO
     */
    private OrderDto convertToWaiterOrder(CookApiClient.CookOrderDto cookOrder, Map<Long, Long> reservationToTableId) {
        OrderDto order = new OrderDto();
        order.id = cookOrder.id;
        order.reservationId = cookOrder.reservationId;

        // Parse table number to get tableId (fallback to reservation mapping for preorders)
        Long tableId = null;
        if (cookOrder.tableNumber != null) {
            try {
                tableId = Long.parseLong(cookOrder.tableNumber);
            } catch (NumberFormatException ignored) {
                tableId = null;
            }
        }
        if ((tableId == null || tableId <= 0) && cookOrder.reservationId != null) {
            tableId = reservationToTableId.get(cookOrder.reservationId);
        }
        order.tableId = tableId != null ? tableId : 0L;

        // Map Cook status to Waiter status
        switch (cookOrder.status) {
            case "PENDING":
            case "IN_KITCHEN":
                order.status = OrderStatus.KUECHE;
                break;
            case "READY":
                order.status = OrderStatus.BEREIT;
                break;
            case "SERVED":
                order.status = OrderStatus.SERVIERT;
                break;
            default:
                order.status = OrderStatus.KUECHE;
        }

        // Map items from Cook API
        if (cookOrder.items != null && !cookOrder.items.isEmpty()) {
            for (CookApiClient.OrderItemDto cookItem : cookOrder.items) {
                order.items.add(new ItemDto(cookItem.name, cookItem.quantity, cookItem.unitPrice));
            }
        } else {
            // Fallback for old orders without items
            order.items.add(new ItemDto("Bestellung #" + cookOrder.id, 1));
        }

        // Map totalPrice
        order.totalPrice = cookOrder.totalPrice;

        return order;
    }

    /**
     * Check-in reservation via Owner API
     */
    public OwnerApiClient.ReservationDto checkInReservation(Long reservationId) {
        return ownerApi.checkIn(reservationId);
    }

    /**
     * Complete reservation and free table
     */
    public OwnerApiClient.ReservationDto completeReservation(Long reservationId) {
        return ownerApi.completeReservation(reservationId);
    }

    /**
     * Create walk-in (customer without reservation)
     */
    public OwnerApiClient.ReservationDto createWalkIn(Long tableId, int numberOfGuests) {
        return ownerApi.createWalkIn(tableId, numberOfGuests);
    }

    /**
     * Process cash payment via Owner API
     * Completes the reservation after payment
     */
    public OwnerApiClient.PaymentDto processCashPayment(Long reservationId, double amount) {
        OwnerApiClient.PaymentDto payment = ownerApi.processCashPayment(reservationId, amount);
        
        // Complete reservation after successful payment
        if (payment != null) {
            try {
                ownerApi.completeReservation(reservationId);
                System.out.println("Reservation " + reservationId + " completed after payment");
            } catch (Exception e) {
                System.err.println("Warning: Could not complete reservation after payment: " + e.getMessage());
            }
        }
        
        return payment;
    }

    /**
     * Process credit card payment via Owner API
     * Completes the reservation after payment
     */
    public OwnerApiClient.PaymentDto processCardPayment(Long reservationId, double amount, String token) {
        OwnerApiClient.PaymentDto payment = ownerApi.processCreditCardPayment(reservationId, amount, token);

        if (payment != null) {
            try {
                ownerApi.completeReservation(reservationId);
                System.out.println("Reservation " + reservationId + " completed after card payment");
            } catch (Exception e) {
                System.err.println("Warning: Could not complete reservation after card payment: " + e.getMessage());
            }
        }

        return payment;
    }

    // ============ ORDER MANAGEMENT ============

    /**
     * Create new order via Cook API
     */
    public CookApiClient.CookOrderDto createOrder(Long tableId, Long reservationId, List<CookApiClient.OrderItemDto> items, Double totalPrice) {
        return cookApi.createOrder(tableId, reservationId, items, totalPrice);
    }

    /**
     * Mark order as served (calls Cook API)
     */
    public void markOrderServed(Long orderId) {
        cookApi.markOrderAsServed(orderId);
        System.out.println("Order " + orderId + " marked as served via Cook API");
    }

    /**
     * Clear table (prepare for cleaning) - calls Owner API
     */
    public void clearTable(Long tableId) {
        OwnerApiClient.TableDto result = ownerApi.clearTable(tableId);
        if (result != null) {
            System.out.println("Table " + tableId + " marked for cleaning via Owner API");
        } else {
            System.err.println("Failed to mark table " + tableId + " for cleaning");
        }
    }

    /**
     * Finish table (mark as available)
     * Completes the active reservation on this table
     */
    public boolean finishTable(Long tableId) {
        try {
            // Find active reservation for this table
            List<OwnerApiClient.ReservationDto> activeReservations = ownerApi.getActiveReservations();
            OwnerApiClient.ReservationDto reservation = activeReservations.stream()
                    .filter(r -> r.tableId != null && r.tableId.equals(tableId))
                    .findFirst()
                    .orElse(null);
            
            // If there's an active reservation, complete it
            if (reservation != null) {
                ownerApi.completeReservation(reservation.id);
                System.out.println("Completed reservation " + reservation.id + " for table " + tableId);
            }
            
            // Always mark table as available
            OwnerApiClient.TableDto result = ownerApi.markTableAvailable(tableId);
            if (result != null) {
                System.out.println("Table " + tableId + " marked as AVAILABLE");
                return true;
            } else {
                System.err.println("Failed to mark table " + tableId + " as available");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error finishing table: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all active reservations
     */
    public List<OwnerApiClient.ReservationDto> getActiveReservations() {
        return ownerApi.getActiveReservations();
    }

    /**
     * Get all tables
     */
    public List<OwnerApiClient.TableDto> getAllTables() {
        return ownerApi.getAllTables();
    }
}
