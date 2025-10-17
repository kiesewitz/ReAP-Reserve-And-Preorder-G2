package at.htlle.reserveandpreordercookg2.model;

import java.time.LocalDateTime;
import java.util.List;

public class Order {

    private String orderId;
    private LocalDateTime orderTime;
    private int tableNumber;
    private List<String> items;
    private double totalPrice;
    private String status; // "Pending" oder "Done"
    private LocalDateTime deliveryTime;
    private String extraInfo;

    public Order() {}

    public Order(String orderId, LocalDateTime orderTime, int tableNumber,
                 List<String> items, double totalPrice, String status,
                 LocalDateTime deliveryTime, String extraInfo) {
        this.orderId = orderId;
        this.orderTime = orderTime;
        this.tableNumber = tableNumber;
        this.items = items;
        this.totalPrice = totalPrice;
        this.status = status;
        this.deliveryTime = deliveryTime;
        this.extraInfo = extraInfo;
    }

    // Getter & Setter
    public String getOrderId() {
        return orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }
    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public int getTableNumber() {
        return tableNumber;
    }
    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
    }

    public List<String> getItems() {
        return items;
    }
    public void setItems(List<String> items) {
        this.items = items;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDeliveryTime() {
        return deliveryTime;
    }
    public void setDeliveryTime(LocalDateTime deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public String getExtraInfo() {
        return extraInfo;
    }
    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }
}
