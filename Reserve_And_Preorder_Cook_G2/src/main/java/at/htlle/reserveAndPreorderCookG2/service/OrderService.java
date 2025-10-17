package at.htlle.reserveAndPreorderCookG2.service;

import at.htlle.reserveandpreordercookg2.model.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderService {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    //Flag, um Demo-Modus zu aktivieren/deaktivieren
    private boolean demoMode = true;

    public OrderService() {
        // Startorder – wird immer angezeigt
        Order example = new Order(
                "100",
                LocalDateTime.now(),
                4,
                List.of("Burger", "Fries"),
                15.90,
                "Pending",
                LocalDateTime.now().plusMinutes(12),
                "Keine Zwiebeln bitte"
        );
        orders.put(example.getOrderId(), example);
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    public boolean markAsDone(String id) {
        Order order = orders.get(id);
        if (order != null) {
            order.setStatus("Done");
            return true;
        }
        return false;
    }

    public void addOrder(Order order) {
        orders.put(order.getOrderId(), order);
    }

    // 👇 Umschalten des Demo-Modus (z. B. per REST)
    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    //Wird jede Minute ausgeführt (1000 * 60 ms)
    @Scheduled(fixedRate = 60000)
    public void generateDemoOrder() {
        if (!demoMode) return; // Nur wenn aktiv

        Random random = new Random();
        int id = random.nextInt(9000) + 1000;

        List<String> items = new ArrayList<>(List.of("Pizza", "Salat", "Pasta", "Wasser", "Burger"));
        Collections.shuffle(items);

        Order demo = new Order(
                "DEMO-" + id,
                LocalDateTime.now(),
                random.nextInt(10) + 1,
                items.subList(0, 2),
                Math.round((Math.random() * 30 + 10) * 100.0) / 100.0,
                "Pending",
                LocalDateTime.now().plusMinutes(random.nextInt(20) + 5),
                "Demo Order automatisch erzeugt"
        );

        orders.put(demo.getOrderId(), demo);
        System.out.println("Neue Demo-Order erstellt: " + demo.getOrderId());
    }

}
