package at.htlle.reserveAndPreorderCookG2.repository;

import at.htlle.reserveAndPreorderCookG2.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find all orders with a specific status
     */
    List<Order> findByStatus(String status);

    /**
     * Find all orders for a specific reservation
     */
    List<Order> findByReservationId(Long reservationId);

    /**
     * Find all orders for a specific table
     */
    List<Order> findByTableNumber(String tableNumber);

    /**
     * Find all preorders
     */
    List<Order> findByIsPreorder(boolean isPreorder);

    /**
     * Find pending and in-kitchen orders (active orders for cook view)
     */
    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'IN_KITCHEN') ORDER BY o.orderDateTime ASC")
    List<Order> findActiveOrders();

    /**
     * Find orders for waiter view (all except CANCELLED)
     */
    @Query("SELECT o FROM Order o WHERE o.status NOT IN ('CANCELLED') ORDER BY o.orderDateTime ASC")
    List<Order> findWaiterOrders();

    /**
     * Find orders ready for serving
     */
    List<Order> findByStatusOrderByOrderDateTimeAsc(String status);
}
