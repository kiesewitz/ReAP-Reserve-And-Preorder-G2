package at.htlle.reserve_and_preorder_g2;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static at.htlle.reserve_and_preorder_g2.WaiterModels.*;

@Service
public class WaiterService {

    private final Map<Long, TableDto> tables = new LinkedHashMap<>();
    private final Map<Long, OrderDto> orders = new LinkedHashMap<>();
    private final AtomicLong ids = new AtomicLong(200);

    public WaiterService() {
        // ---- Demo-Tische (unverändert wie ursprünglich) ----
        tables.put(1L, new TableDto(1L, "Tisch 1", 4, TableStatus.BESTELLUNG_FERTIG));
        tables.put(2L, new TableDto(2L, "Tisch 2", 2, TableStatus.BELEGT));
        tables.put(3L, new TableDto(3L, "Tisch 3", 6, TableStatus.LEER));
        tables.put(4L, new TableDto(4L, "Tisch 4", 4, TableStatus.BESTELLUNG_FERTIG));
        tables.put(5L, new TableDto(5L, "Tisch 5", 8, TableStatus.BELEGT));
        tables.put(6L, new TableDto(6L, "Tisch 6", 2, TableStatus.ESSEN));

        // ---- Demo-Bestellungen (unverändert wie ursprünglich) ----
        OrderDto o101 = new OrderDto();
        o101.id = 101L; o101.tableId = 1L; o101.status = OrderStatus.BEREIT;
        o101.items.add(new ItemDto("Margherita", 1));
        o101.items.add(new ItemDto("Apfelschorle", 2));
        orders.put(o101.id, o101);

        OrderDto o102 = new OrderDto();
        o102.id = 102L; o102.tableId = 2L; o102.status = OrderStatus.KUECHE;
        o102.items.add(new ItemDto("Caesar Salad", 1));
        o102.items.add(new ItemDto("Mineralwasser", 1));
        orders.put(o102.id, o102);

        OrderDto o103 = new OrderDto();
        o103.id = 103L; o103.tableId = 4L; o103.status = OrderStatus.BEREIT;
        o103.items.add(new ItemDto("Tagliatelle", 2));
        o103.items.add(new ItemDto("Espresso", 2));
        orders.put(o103.id, o103);

        OrderDto o104 = new OrderDto();
        o104.id = 104L; o104.tableId = 5L; o104.status = OrderStatus.KUECHE;
        o104.items.add(new ItemDto("Pizza Diavolo", 1));
        o104.items.add(new ItemDto("Cola", 2));
        orders.put(o104.id, o104);

        OrderDto o105 = new OrderDto();
        o105.id = 105L; o105.tableId = 6L; o105.status = OrderStatus.SERVIERT;
        o105.items.add(new ItemDto("Wiener Schnitzel", 2));
        o105.items.add(new ItemDto("Pommes", 2));
        o105.items.add(new ItemDto("Radler", 2));
        orders.put(o105.id, o105);

        // Status der Tische initial anhand der Bestellungen konsistent setzen:
        recalcTableStatusAll();
    }

    // ---- Öffentliche API für Controller ----
    public WaiterStateDto getState() {
        var listTables = new ArrayList<>(tables.values());
        var listOrders = new ArrayList<>(orders.values());
        return new WaiterStateDto(listTables, listOrders);
    }

    public void markOrderServed(Long orderId) {
        var o = orders.get(orderId);
        if (o == null) return;
        o.status = OrderStatus.SERVIERT;
        recalcTableStatus(o.tableId);
    }

    /** „Tisch abservieren“ = Gäste fertig, Kellner markiert, Tisch muss abgeräumt werden */
    public void clearTable(Long tableId) {
        var t = tables.get(tableId);
        if (t != null && t.status == TableStatus.ESSEN) {
            t.status = TableStatus.ABSERVIEREN;
        }
    }

    /**
     * „Tisch fertig“ = Tisch abgeräumt → LEER
     * Rückgabewert: true = erfolgreich gesetzt, false = nicht erlaubt (z.B. noch BEREIT-Bestellungen oder falscher Status)
     */
    public boolean finishTable(Long tableId) {
        var t = tables.get(tableId);
        if (t == null) return false;

        // Wenn es noch Bestellungen mit Status BEREIT für diesen Tisch gibt, darf nicht fertig gesetzt werden.
        boolean anyReady = orders.values().stream()
                .anyMatch(o -> Objects.equals(o.tableId, tableId) && o.status == OrderStatus.BEREIT);
        if (anyReady) {
            return false; // nicht erlaubt
        }

        // Nur erlauben, wenn Tisch zuvor abserviert wurde (oder du möchtest weitere Regeln, passe hier an)
        if (t.status == TableStatus.ABSERVIEREN) {
            t.status = TableStatus.LEER;
            return true;
        }

        // Falls du möchtest, dass finish auch erlaubt ist, wenn keine Bestellungen existieren,
        // könntest du hier z.B. erlauben, wenn keine Bestellungen vorhanden sind.
        // Aktuell: nur ABSERVIEREN -> LEER erlaubt.
        return false;
    }

    // ---- Hilfslogik für Status-Berechnung ----
    private void recalcTableStatusAll() {
        tables.keySet().forEach(this::recalcTableStatus);
    }

    private void recalcTableStatus(Long tableId) {
        boolean anyReady = orders.values().stream()
                .anyMatch(o -> Objects.equals(o.tableId, tableId) && o.status == OrderStatus.BEREIT);
        boolean anyKitchen = orders.values().stream()
                .anyMatch(o -> Objects.equals(o.tableId, tableId) && o.status == OrderStatus.KUECHE);
        boolean allServedOrNone = orders.values().stream()
                .filter(o -> Objects.equals(o.tableId, tableId))
                .allMatch(o -> o.status == OrderStatus.SERVIERT);

        var t = tables.get(tableId);
        if (t == null) return;

        if (anyReady) {
            t.status = TableStatus.BESTELLUNG_FERTIG;   // To-do für Kellner
        } else if (anyKitchen) {
            t.status = TableStatus.BELEGT;              // Gäste warten, Kellner hat kein To-do
        } else if (allServedOrNone) {
            // Es gibt entweder keine Bestellungen, oder alle sind serviert → Gäste essen
            // WICHTIG: Wenn Tisch aktuell ABSERVIEREN oder LEER ist, lassen wir den Status unverändert.
            if (t.status != TableStatus.ABSERVIEREN && t.status != TableStatus.LEER) {
                t.status = TableStatus.ESSEN;
            }
        } else {
            // Fallback
            t.status = TableStatus.BELEGT;
        }
    }
}
