package at.htlle.reserveAndPreorderCookG2.service;

import at.htlle.reserveAndPreorderCookG2.dto.PreorderRequest;
import at.htlle.reserveAndPreorderCookG2.model.Order;
import at.htlle.reserveAndPreorderCookG2.model.OrderItem;
import at.htlle.reserveAndPreorderCookG2.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    // Flag for demo mode (auto-generate orders) - DISABLED by default
    private boolean demoMode = false;

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        System.out.println("Order service initialized. Current orders: " + orderRepository.count());
    }

    /**
     * Get all orders (for cook dashboard)
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Get only active orders (PENDING and IN_KITCHEN)
     */
    public List<Order> getActiveOrders() {
        return orderRepository.findActiveOrders();
    }

    /**
     * Get orders for waiter (PENDING, IN_KITCHEN, and READY)
     */
    public List<Order> getWaiterOrders() {
        return orderRepository.findWaiterOrders();
    }

    /**
     * Get orders by status
     */
    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    /**
     * Get order by ID
     */
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    /**
     * Create new order
     */
    @Transactional
    public Order createOrder(Order order) {
        if (order.getStatus() == null) {
            order.setStatus("PENDING");
        }
        Order saved = orderRepository.save(order);
        System.out.println("Order created: " + saved);
        return saved;
    }

    /**
     * Mark order as "IN_KITCHEN" (cook started working on it)
     */
    @Transactional
    public Order markAsInKitchen(Long id) {
        Order order = getOrderById(id);
        order.setStatus("IN_KITCHEN");
        Order updated = orderRepository.save(order);
        System.out.println("Order " + id + " marked as IN_KITCHEN");
        return updated;
    }

    /**
     * Mark order as "READY" (cook finished, ready for serving)
     */
    @Transactional
    public Order markAsReady(Long id) {
        Order order = getOrderById(id);
        order.setStatus("READY");
        order.setDeliveryTime(LocalDateTime.now());
        Order updated = orderRepository.save(order);
        System.out.println("Order " + id + " marked as READY");
        return updated;
    }

    /**
     * Mark order as "SERVED" (waiter delivered to customer)
     */
    @Transactional
    public Order markAsServed(Long id) {
        Order order = getOrderById(id);
        order.setStatus("SERVED");
        Order updated = orderRepository.save(order);
        System.out.println("Order " + id + " marked as SERVED");
        return updated;
    }

    /**
     * Cancel order
     */
    @Transactional
    public Order cancelOrder(Long id) {
        Order order = getOrderById(id);
        order.setStatus("CANCELLED");
        Order updated = orderRepository.save(order);
        System.out.println("Order " + id + " cancelled");
        return updated;
    }

    /**
     * Legacy method for backward compatibility - mark as done (maps to READY)
     */
    @Transactional
    public boolean markAsDone(String id) {
        try {
            Long orderId = Long.parseLong(id);
            markAsReady(orderId);
            return true;
        } catch (Exception e) {
            System.err.println("Error marking order as done: " + e.getMessage());
            return false;
        }
    }

    /**
     * Toggle demo mode
     */
    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
        System.out.println("Demo mode set to: " + demoMode);
    }

    /**
     * Auto-generate demo orders every minute (only in demo mode)
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void generateDemoOrder() {
        if (!demoMode) return;

        Random random = new Random();
        int tableNum = random.nextInt(10) + 1;
        double price = Math.round((Math.random() * 30 + 10) * 100.0) / 100.0;

        Order demo = new Order(null, String.valueOf(tableNum), BigDecimal.valueOf(price));
        demo.setStatus("PENDING");
        demo.setSpecialRequests("Demo Order automatisch erzeugt");
        demo.setDeliveryTime(LocalDateTime.now().plusMinutes(random.nextInt(20) + 5));

        orderRepository.save(demo);
        System.out.println("Neue Demo-Order erstellt: Tisch " + tableNum + ", " + price + "€");
    }

    // ==================== PREORDER METHODS ====================

    /**
     * Create a preorder from customer reservation
     */
    @Transactional
    public Order createPreorder(PreorderRequest request) {
        Order preorder = new Order();
        preorder.setReservationId(request.getReservationId());
        preorder.setTableNumber(request.getTableNumber() != null ? request.getTableNumber() : "TBD");
        preorder.setPreorder(true);
        preorder.setStatus("PENDING");
        preorder.setSpecialRequests(request.getSpecialRequests());
        preorder.setDeliveryTime(request.getDeliveryTime());

        // Calculate total price and add items
        BigDecimal totalPrice = BigDecimal.ZERO;

        if (request.getItems() != null) {
            for (PreorderRequest.PreorderItemDto itemDto : request.getItems()) {
                OrderItem item = new OrderItem();
                item.setName(itemDto.getName());
                item.setQuantity(itemDto.getQuantity());
                item.setMenuItemId(itemDto.getMenuItemId());
                item.setUnitPrice(itemDto.getUnitPrice());
                item.setSpecialInstructions(itemDto.getSpecialInstructions());

                preorder.addItem(item);

                // Calculate total
                if (itemDto.getUnitPrice() != null) {
                    BigDecimal itemTotal = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                    totalPrice = totalPrice.add(itemTotal);
                }
            }
        }

        preorder.setTotalPrice(totalPrice);

        Order saved = orderRepository.save(preorder);
        System.out.println("Preorder erstellt: Reservierung " + request.getReservationId() + ", Summe: " + totalPrice + "€");
        return saved;
    }

    /**
     * Get all preorders
     */
    public List<Order> getPreorders() {
        return orderRepository.findByIsPreorder(true);
    }

    /**
     * Get orders by reservation ID
     */
    public List<Order> getOrdersByReservation(Long reservationId) {
        return orderRepository.findByReservationId(reservationId);
    }
}
