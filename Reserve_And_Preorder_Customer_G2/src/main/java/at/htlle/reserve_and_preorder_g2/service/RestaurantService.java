package at.htlle.reserve_and_preorder_g2.service;

import at.htlle.reserve_and_preorder_g2.pojo.Restaurant;
import at.htlle.reserve_and_preorder_g2.repository.RestaurantRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Autowired
    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @PostConstruct
    public void initDummyData() {
        // Nur ein Restaurant behalten (Mischung aus allen)
        if (restaurantRepository.count() != 1) {
            restaurantRepository.deleteAll();
            restaurantRepository.save(new Restaurant(null, "ReAP Fusion Kitchen – Pizza • Grill • Kebab",
                "Laktosefreie Pizza, saftige Fleischgerichte und würziger Kebab – das Beste aus allen drei Küchen vereint",
                "Mibombostraße 15, 1234 Mibombo City",
                "+43 677 1233321478"));
        }
    }

    /**
     * Saves a Restaurant object to the database
     * @param newRestaurant
     */
    public Restaurant saveRestaurant(Restaurant newRestaurant) {
        Restaurant saved = restaurantRepository.save(newRestaurant);
        System.out.println("Saved Restaurant: " + saved.toString());
        return saved;
    }

    /**
     * Gets a Restaurant object from the database
     * @param id
     * @return
     */
    public Restaurant getRestaurant(Long id) {
        return restaurantRepository.findById(id).orElse(null);
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    /**
     * Deletes a Restaurant object from the database
     * @param id
     */
    public void deleteRestaurant(Long id) {
        restaurantRepository.deleteById(id);
    }
}
