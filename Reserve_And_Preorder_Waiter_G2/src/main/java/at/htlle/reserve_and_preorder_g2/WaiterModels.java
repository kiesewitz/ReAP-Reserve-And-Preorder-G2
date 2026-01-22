package at.htlle.reserve_and_preorder_g2;

import java.util.ArrayList;
import java.util.List;

public class WaiterModels {

    // ---- Deutsche Tisch-Status ----
    public enum TableStatus {
        LEER,               // Tisch ist frei/abgeräumt
        RESERVIERT,         // Reserviert aber noch nicht eingecheckt
        BELEGT,             // Gäste sitzen, aber kein To-do
        BESTELLUNG_FERTIG,  // Bestellung steht bereit, Kellner soll austragen
        ESSEN,              // serviert, Gäste essen noch
        ABSERVIEREN         // Gäste fertig, Tisch muss abgeräumt werden
    }

    // ---- Deutsche Bestell-Status ----
    public enum OrderStatus {
        KUECHE,   // in Zubereitung
        BEREIT,   // fertig zum Austragen
        SERVIERT  // am Tisch serviert
    }

    public static class TableDto {
        public Long id;
        public Long restaurantId;
        public String name;
        public Integer seats;
        public Integer capacity;  // Added for API compatibility
        public TableStatus status;
        public Long currentReservationId;  // Added for tracking

        public TableDto() {}

        public TableDto(Long id, String name, Integer seats, TableStatus status) {
            this.id = id;
            this.name = name;
            this.seats = seats;
            this.capacity = seats;
            this.status = status;
        }
    }

    public static class ItemDto {
        public String name;
        public Integer qty;
        public Double unitPrice;

        public ItemDto() {}

        public ItemDto(String name, Integer qty) {
            this.name = name;
            this.qty = qty;
        }

        public ItemDto(String name, Integer qty, Double unitPrice) {
            this.name = name;
            this.qty = qty;
            this.unitPrice = unitPrice;
        }
    }

    public static class OrderDto {
        public Long id;
        public Long reservationId;
        public Long tableId;
        public OrderStatus status;
        public List<ItemDto> items = new ArrayList<>();
        public Double totalPrice;
    }

    public static class WaiterStateDto {
        public List<TableDto> tables;
        public List<OrderDto> orders;

        public WaiterStateDto(List<TableDto> tables, List<OrderDto> orders) {
            this.tables = tables;
            this.orders = orders;
        }
    }
}
