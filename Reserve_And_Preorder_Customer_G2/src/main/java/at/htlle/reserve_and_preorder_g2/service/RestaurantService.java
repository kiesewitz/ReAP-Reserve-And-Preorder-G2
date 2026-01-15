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
        // Nur Dummy-Daten hinzufügen, wenn DB leer ist
        if (restaurantRepository.count() == 0) {
            restaurantRepository.save(new Restaurant(null, "André's Pizzeria",
                "Pizza mit laktosefreiem Käse",
                "Mibombostraße 15, 1234 Mibombo City",
                "+43 677 1233321478"));
            restaurantRepository.save(new Restaurant(null, "Nikita's Meat",
                "Essen für Fleischliebhaber",
                "Mibombostraße 17, 1234 Mibombo City",
                "+43 677 345543192"));
            restaurantRepository.save(new Restaurant(null, "Mojo's Kebab",
                "Kebab mit spezieller Sauce (von Mateusel)",
                "Clasher-Straße 75, 1234 Mibombo City",
                "+43 677 094618941"));
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
