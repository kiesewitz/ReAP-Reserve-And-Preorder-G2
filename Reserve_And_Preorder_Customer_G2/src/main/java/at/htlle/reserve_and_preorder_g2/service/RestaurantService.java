package at.htlle.reserve_and_preorder_g2.service;

import at.htlle.reserve_and_preorder_g2.pojo.Restaurant;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RestaurantService {
    List<Restaurant> allRestaurants = new ArrayList<>();

    public RestaurantService() {
        // Add dummy Widgets
        allRestaurants.add(new Restaurant(1, "André's Pizzeria", "Pizza mit laktosefreiem Käse", "Mibombostraße 15, 1234 Mibombo City", "+43 677 1233321478"));
        allRestaurants.add(new Restaurant(2, "Nikita's Meat", "Essen für Fleischliebhaber", "Mibombostraße 17, 1234 Mibombo City", "+43 677 345543192"));
        allRestaurants.add(new Restaurant(3, "Mojo's Kebab", "Kebab mit spezieller Sauce (von Mateusel)", "Clasher-Straße 75, 1234 Mibombo City", "+43 677 094618941"));
    }

    /**
     * Saves a Restaurant object to the list
     * @param newRestaurant
     */
    public void saveRestaurant(Restaurant newRestaurant) {
        this.allRestaurants.add(newRestaurant);
        System.out.println("Saved Restaurant: " + newRestaurant.toString());
    }

    /**
     * Gets a Restaurant object from the list
     * @param idx
     * @return
     */
    public Restaurant getRestaurant(Integer idx) {
        return this.allRestaurants.stream()
                .filter(t -> t.getId() == idx)
                .findFirst().orElse(null);
    }

    public List<Restaurant> getAllRestaurants() {
        return this.allRestaurants;
    }

    /**
     * Deletes a Restaurant object from the list
     * @param idx
     * @return
     */
    public void deleteRestaurant(Integer idx) {
        this.allRestaurants.remove(idx.intValue());
    }
}
