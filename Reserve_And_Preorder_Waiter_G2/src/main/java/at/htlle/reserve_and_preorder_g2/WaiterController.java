package at.htlle.reserve_and_preorder_g2;

import at.htlle.reserve_and_preorder_g2.WaiterModels.WaiterStateDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WaiterController {

    private final WaiterService service;

    public WaiterController(WaiterService service) {
        this.service = service;
    }

    // (1) Übersicht: Welche Bestellungen auf welchem Tisch?
    @GetMapping("/waiter/state")
    public WaiterStateDto state() {
        return service.getState();
    }

    // (2a) Bestellung als serviert markieren
    @PostMapping("/orders/{orderId}/served")
    public ResponseEntity<Void> served(@PathVariable Long orderId) {
        service.markOrderServed(orderId);
        return ResponseEntity.noContent().build();
    }

    // (2b) Tisch abservieren (nach dem Essen)
    @PostMapping("/tables/{tableId}/clear")
    public ResponseEntity<Void> clear(@PathVariable Long tableId) {
        service.clearTable(tableId);
        return ResponseEntity.noContent().build();
    }

    // (2c) gesamten Tisch als "fertig" markieren
    @PostMapping("/tables/{tableId}/finish")
    public ResponseEntity<Void> finish(@PathVariable Long tableId) {
        boolean ok = service.finishTable(tableId);
        if (ok) {
            return ResponseEntity.noContent().build();
        } else {
            // 409 Conflict: Aktion nicht erlaubt (z.B. noch BEREIT-Bestellungen oder falscher Status)
            return ResponseEntity.status(409).build();
        }
    }
}
