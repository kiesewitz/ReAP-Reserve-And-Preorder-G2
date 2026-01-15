package at.htlle.reap.model;

import at.htlle.reap.enums.TableStatus;
import jakarta.persistence.*;

@Entity
@jakarta.persistence.Table(name = "tables")
public class Table {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "table_number", nullable = false, length = 10)
    private String tableNumber;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(name = "current_reservation_id")
    private Long currentReservationId;

    // No-arg constructor for JPA
    public Table() {
    }

    // Constructor with essential fields
    public Table(Long restaurantId, String tableNumber, int capacity) {
        this.restaurantId = restaurantId;
        this.tableNumber = tableNumber;
        this.capacity = capacity;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public TableStatus getStatus() {
        return status;
    }

    public void setStatus(TableStatus status) {
        this.status = status;
    }

    public Long getCurrentReservationId() {
        return currentReservationId;
    }

    public void setCurrentReservationId(Long currentReservationId) {
        this.currentReservationId = currentReservationId;
    }

    @Override
    public String toString() {
        return "Table{" +
                "id=" + id +
                ", restaurantId=" + restaurantId +
                ", tableNumber='" + tableNumber + '\'' +
                ", capacity=" + capacity +
                ", status=" + status +
                '}';
    }
}
