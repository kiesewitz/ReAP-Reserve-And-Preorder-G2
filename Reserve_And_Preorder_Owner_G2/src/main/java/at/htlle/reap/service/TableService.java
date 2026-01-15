package at.htlle.reap.service;

import at.htlle.reap.enums.TableStatus;
import at.htlle.reap.model.Table;
import at.htlle.reap.repository.TableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TableService {

    private final TableRepository tableRepository;

    @Autowired
    public TableService(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    // Table initialization removed - tables should be created manually via API

    /**
     * Get all tables
     */
    public List<Table> getAllTables() {
        return tableRepository.findAll();
    }

    /**
     * Get table by ID
     */
    public Table getTableById(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found with id: " + id));
    }

    /**
     * Get tables by restaurant ID
     */
    public List<Table> getTablesByRestaurantId(Long restaurantId) {
        return tableRepository.findByRestaurantId(restaurantId);
    }

    /**
     * Alias for getTablesByRestaurantId (for API compatibility)
     */
    public List<Table> getTablesByRestaurant(Long restaurantId) {
        return getTablesByRestaurantId(restaurantId);
    }

    /**
     * Get available tables with sufficient capacity
     */
    public List<Table> getAvailableTablesWithCapacity(Long restaurantId, int minCapacity) {
        return tableRepository.findAvailableTablesWithCapacity(restaurantId, minCapacity);
    }

    /**
     * Update table status
     */
    public Table updateTableStatus(Long tableId, TableStatus newStatus) {
        Table table = getTableById(tableId);
        table.setStatus(newStatus);
        return tableRepository.save(table);
    }

    /**
     * Create a new table
     */
    public Table createTable(Table table) {
        return tableRepository.save(table);
    }

    /**
     * Update an existing table
     */
    public Table updateTable(Table table) {
        if (table.getId() == null) {
            throw new RuntimeException("Table ID must not be null for update");
        }
        return tableRepository.save(table);
    }

    /**
     * Delete a table (returns boolean for controller compatibility)
     */
    public boolean deleteTable(Long id) {
        if (tableRepository.existsById(id)) {
            tableRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Mark table for cleaning
     */
    public Table markForCleaning(Long tableId) {
        return updateTableStatus(tableId, TableStatus.CLEANING);
    }

    /**
     * Mark table as available
     */
    public Table markAsAvailable(Long tableId) {
        Table table = getTableById(tableId);
        table.setStatus(TableStatus.AVAILABLE);
        table.setCurrentReservationId(null);
        return tableRepository.save(table);
    }

    /**
     * Assign reservation to table
     * Validates that the table is not already assigned to a different reservation
     */
    public Table assignReservation(Long tableId, Long reservationId) {
        Table table = getTableById(tableId);
        
        // Check if table is already assigned to a different active reservation
        if (table.getCurrentReservationId() != null && 
            !table.getCurrentReservationId().equals(reservationId)) {
            // Table is assigned to a different reservation
            if (table.getStatus() == TableStatus.RESERVED || 
                table.getStatus() == TableStatus.OCCUPIED) {
                throw new RuntimeException("Tisch " + table.getTableNumber() + 
                    " ist bereits einer anderen Reservierung zugewiesen (Reservierung #" + 
                    table.getCurrentReservationId() + "). Bitte wählen Sie einen anderen Tisch.");
            }
        }
        
        // Assign table to reservation
        table.setStatus(TableStatus.RESERVED);
        table.setCurrentReservationId(reservationId);
        return tableRepository.save(table);
    }
}
