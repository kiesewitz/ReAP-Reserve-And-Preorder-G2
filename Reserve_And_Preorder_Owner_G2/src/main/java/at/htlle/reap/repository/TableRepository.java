package at.htlle.reap.repository;

import at.htlle.reap.enums.TableStatus;
import at.htlle.reap.model.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TableRepository extends JpaRepository<Table, Long> {

    // Find tables by restaurant ID
    List<Table> findByRestaurantId(Long restaurantId);

    // Find tables by restaurant and status
    List<Table> findByRestaurantIdAndStatus(Long restaurantId, TableStatus status);

    // Find available tables with sufficient capacity
    @Query("SELECT t FROM Table t WHERE t.restaurantId = :restaurantId " +
           "AND t.status = 'AVAILABLE' " +
           "AND t.capacity >= :minCapacity " +
           "ORDER BY t.capacity ASC")
    List<Table> findAvailableTablesWithCapacity(@Param("restaurantId") Long restaurantId,
                                                @Param("minCapacity") int minCapacity);
}
