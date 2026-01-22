package at.htlle.reap.controller;

import at.htlle.reap.model.MenuItem;
import at.htlle.reap.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuController {

    @Autowired
    private MenuService menuService;

    // Alle Menü-Items eines Restaurants abrufen
    @GetMapping("/{restaurantId}")
    public ResponseEntity<List<MenuItem>> getMenuByRestaurant(@PathVariable Long restaurantId) {
        List<MenuItem> menu = menuService.getMenuByRestaurant(restaurantId);
        return ResponseEntity.ok(menu);
    }

    // Nur verfügbare Menü-Items eines Restaurants
    @GetMapping("/{restaurantId}/available")
    public ResponseEntity<List<MenuItem>> getAvailableMenuItems(@PathVariable Long restaurantId) {
        List<MenuItem> availableItems = menuService.getAvailableMenuItems(restaurantId);
        return ResponseEntity.ok(availableItems);
    }

    // Menü-Items nach Kategorie filtern
    @GetMapping("/{restaurantId}/category/{category}")
    public ResponseEntity<List<MenuItem>> getMenuItemsByCategory(
            @PathVariable Long restaurantId,
            @PathVariable String category) {
        List<MenuItem> items = menuService.getMenuItemsByCategory(restaurantId, category);
        return ResponseEntity.ok(items);
    }

    // Einzelnes Menü-Item abrufen
    @GetMapping("/item/{id}")
    public ResponseEntity<MenuItem> getMenuItemById(@PathVariable Long id) {
        return menuService.getMenuItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Neues Menü-Item erstellen
    @PostMapping
    public ResponseEntity<MenuItem> createMenuItem(@RequestBody MenuItem menuItem) {
        MenuItem created = menuService.createMenuItem(menuItem);
        return ResponseEntity.ok(created);
    }

    // Menü-Item aktualisieren
    @PutMapping("/{id}")
    public ResponseEntity<MenuItem> updateMenuItem(@PathVariable Long id, @RequestBody MenuItem menuItem) {
        try {
            MenuItem updated = menuService.updateMenuItem(id, menuItem);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Menü-Item löschen
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        menuService.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }

    // Verfügbarkeit umschalten
    @PatchMapping("/{id}/availability")
    public ResponseEntity<MenuItem> toggleAvailability(@PathVariable Long id) {
        try {
            MenuItem updated = menuService.toggleAvailability(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
