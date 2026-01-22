package at.htlle.reap.repository;

import at.htlle.reap.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByRestaurantId(Long restaurantId);

    List<MenuItem> findByRestaurantIdAndIsAvailable(Long restaurantId, boolean isAvailable);

    List<MenuItem> findByRestaurantIdAndCategory(Long restaurantId, String category);

    List<MenuItem> findByRestaurantIdAndCategoryAndIsAvailable(Long restaurantId, String category, boolean isAvailable);

    List<MenuItem> findByRestaurantIdOrderByCategoryAscNameAsc(Long restaurantId);
}
