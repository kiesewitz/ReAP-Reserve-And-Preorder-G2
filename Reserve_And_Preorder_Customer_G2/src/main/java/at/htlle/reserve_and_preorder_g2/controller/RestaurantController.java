package at.htlle.reserve_and_preorder_g2.controller;

import at.htlle.reserve_and_preorder_g2.pojo.Restaurant;
import at.htlle.reserve_and_preorder_g2.service.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class RestaurantController {
    @Autowired
    RestaurantService service;

    @PostMapping(value = "/restaurant")
    public ResponseEntity<Restaurant> createWidget(@RequestBody Restaurant newRestaurant) throws IOException {
        System.out.println("Creating new Restaurant: " + newRestaurant.toString());
        service.saveRestaurant(newRestaurant);
        return ResponseEntity.ok(newRestaurant);
    }

    @GetMapping(value = "/restaurant/{id}", produces = "application/json")
    ResponseEntity<Restaurant> readWidget(@PathVariable(value = "id") Long id) {
        return ResponseEntity.ok(service.getRestaurant(id));
    }

    @GetMapping(value = "/restaurant", produces = "application/json")
    ResponseEntity<List<Restaurant>> readAllRestaurants() {
        return ResponseEntity.ok(service.getAllRestaurants());
    }

    @DeleteMapping("/restaurant/{id}")
    void deleteToDo(@PathVariable(value = "id") Long id) {
        service.deleteRestaurant(id);
    }
}
