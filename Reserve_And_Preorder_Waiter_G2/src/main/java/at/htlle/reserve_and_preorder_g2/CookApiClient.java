package at.htlle.reserve_and_preorder_g2;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Client to communicate with Cook App API (port 8081)
 */
@Service
public class CookApiClient {

    private final RestTemplate restTemplate;
    private final String COOK_API_BASE = "http://localhost:8081/api";

    public CookApiClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get all active orders from Cook app (for waiter)
     * Uses /waiter endpoint which filters PENDING, IN_KITCHEN, READY only
     */
    public List<CookOrderDto> getActiveOrders() {
        try {
            String url = COOK_API_BASE + "/orders/waiter";
            ResponseEntity<List<CookOrderDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<CookOrderDto>>() {}
            );
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error fetching orders from Cook API: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Mark order as served (called by Waiter)
     */
    public void markOrderAsServed(Long orderId) {
        try {
            String url = COOK_API_BASE + "/orders/" + orderId + "/served";
            restTemplate.postForObject(url, null, Void.class);
        } catch (Exception e) {
            System.err.println("Error marking order as served: " + e.getMessage());
        }
    }

    /**
     * Create new order
     */
    public CookOrderDto createOrder(Long tableId, Long reservationId, List<OrderItemDto> items, Double totalPrice) {
        try {
            CreateOrderRequest request = new CreateOrderRequest();
            request.tableId = tableId;
            request.reservationId = reservationId;
            request.items = items;
            request.status = "PENDING";
            request.totalPrice = totalPrice;

            String url = COOK_API_BASE + "/orders";
            ResponseEntity<CookOrderDto> response = restTemplate.postForEntity(
                url,
                request,
                CookOrderDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error creating order: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * DTO for Cook orders
     */
    public static class CookOrderDto {
        public Long id;
        public Long reservationId;
        public String tableNumber;
        public String orderDateTime;
        public Double totalPrice;
        public String status; // PENDING, IN_KITCHEN, READY, SERVED
        public String specialRequests;
        public String deliveryTime;
        public List<OrderItemDto> items;
    }

    /**
     * DTO for Order Items
     */
    public static class OrderItemDto {
        public String name;
        public int quantity;
        public Long menuItemId;
        public Double unitPrice;
        public String specialInstructions;

        public OrderItemDto() {}

        public OrderItemDto(String name, int quantity) {
            this.name = name;
            this.quantity = quantity;
        }

        public OrderItemDto(String name, int quantity, Long menuItemId, Double unitPrice, String specialInstructions) {
            this.name = name;
            this.quantity = quantity;
            this.menuItemId = menuItemId;
            this.unitPrice = unitPrice;
            this.specialInstructions = specialInstructions;
        }

    }

    /**
     * DTO for creating new order
     */
    public static class CreateOrderRequest {
        public Long reservationId;
        public Long tableId;
        public List<OrderItemDto> items;
        public String status;
        public Double totalPrice;
    }
}
