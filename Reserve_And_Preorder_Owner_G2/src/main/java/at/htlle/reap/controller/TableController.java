package at.htlle.reap.controller;

import at.htlle.reap.model.Table;
import at.htlle.reap.service.TableService;
import at.htlle.reap.service.ResService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/tables")
public class TableController {

    private final TableService tableService;
    private final ResService resService;

    @Autowired
    public TableController(TableService tableService, ResService resService) {
        this.tableService = tableService;
        this.resService = resService;
    }

    /**
     * Get all tables
     */
    @GetMapping
    public ResponseEntity<List<Table>> getAllTables() {
        return ResponseEntity.ok(tableService.getAllTables());
    }

    /**
     * Get tables by restaurant ID
     */
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<Table>> getTablesByRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(tableService.getTablesByRestaurant(restaurantId));
    }

    /**
     * Get single table by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Table> getTableById(@PathVariable Long id) {
        Table table = tableService.getTableById(id);
        if (table != null) {
            return ResponseEntity.ok(table);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Create new table
     */
    @PostMapping
    public ResponseEntity<Table> createTable(@RequestBody Table table) {
        Table created = tableService.createTable(table);
        return ResponseEntity.ok(created);
    }

    /**
     * Update table
     */
    @PutMapping("/{id}")
    public ResponseEntity<Table> updateTable(@PathVariable Long id, @RequestBody Table table) {
        table.setId(id);
        Table updated = tableService.updateTable(table);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Delete table
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(@PathVariable Long id) {
        boolean deleted = tableService.deleteTable(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Mark table for cleaning
     */
    @PostMapping("/{id}/clear")
    public ResponseEntity<Table> clearTable(@PathVariable Long id) {
        Table table = tableService.markForCleaning(id);
        if (table != null) {
            return ResponseEntity.ok(table);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Mark table as available
     */
    @PostMapping("/{id}/available")
    public ResponseEntity<Table> markAvailable(@PathVariable Long id) {
        Table table = tableService.markAsAvailable(id);
        if (table != null) {
            return ResponseEntity.ok(table);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Assign reservation to table
     */
    @PostMapping("/{id}/assign/{reservationId}")
    public ResponseEntity<Table> assignReservation(
            @PathVariable Long id,
            @PathVariable Long reservationId) {
        try {
            Table table = tableService.assignReservation(id, reservationId);
            
            // Update reservation with tableId
            resService.updateReservationTable(reservationId, id);
            
            return ResponseEntity.ok(table);
        } catch (RuntimeException e) {
            System.err.println("Error assigning table: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
