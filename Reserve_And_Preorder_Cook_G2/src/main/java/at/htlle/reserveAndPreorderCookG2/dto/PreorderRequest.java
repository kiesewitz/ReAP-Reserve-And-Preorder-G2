package at.htlle.reserveAndPreorderCookG2.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PreorderRequest {

    private Long reservationId;
    private Long restaurantId;
    private String tableNumber;
    private List<PreorderItemDto> items;
    private String specialRequests;
    private LocalDateTime deliveryTime;

    // Constructors
    public PreorderRequest() {}

    // Getters and Setters
    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public List<PreorderItemDto> getItems() {
        return items;
    }

    public void setItems(List<PreorderItemDto> items) {
        this.items = items;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public LocalDateTime getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(LocalDateTime deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    // Inner class for preorder items
    public static class PreorderItemDto {
        private Long menuItemId;
        private String name;
        private int quantity;
        private BigDecimal unitPrice;
        private String specialInstructions;

        public PreorderItemDto() {}

        public Long getMenuItemId() {
            return menuItemId;
        }

        public void setMenuItemId(Long menuItemId) {
            this.menuItemId = menuItemId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public String getSpecialInstructions() {
            return specialInstructions;
        }

        public void setSpecialInstructions(String specialInstructions) {
            this.specialInstructions = specialInstructions;
        }
    }
}
