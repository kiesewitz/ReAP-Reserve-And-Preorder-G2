package at.htlle.reserve_and_preorder_g2.repository;

import at.htlle.reserve_and_preorder_g2.pojo.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    // Custom queries können hier hinzugefügt werden
}
