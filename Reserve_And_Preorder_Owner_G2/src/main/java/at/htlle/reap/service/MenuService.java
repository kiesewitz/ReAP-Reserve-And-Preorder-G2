package at.htlle.reap.service;

import at.htlle.reap.model.MenuItem;
import at.htlle.reap.repository.MenuItemRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class MenuService {

    @Autowired
    private MenuItemRepository menuItemRepository;

    @PostConstruct
    public void initMenuItems() {
        // Nur initialisieren, wenn keine Items vorhanden
        if (menuItemRepository.count() == 0) {
            Long restaurantId = 1L;

            // === VORSPEISEN (6 Stück) ===
            menuItemRepository.save(new MenuItem(restaurantId, "Bruschetta",
                "Geröstetes Brot mit frischen Tomaten, Knoblauch und Basilikum",
                new BigDecimal("6.90"), "Vorspeise", 10));

            menuItemRepository.save(new MenuItem(restaurantId, "Minestrone",
                "Klassische italienische Gemüsesuppe mit saisonalem Gemüse",
                new BigDecimal("5.50"), "Vorspeise", 15));

            menuItemRepository.save(new MenuItem(restaurantId, "Caprese Salat",
                "Frische Tomaten mit Büffelmozzarella, Basilikum und Balsamico",
                new BigDecimal("8.50"), "Vorspeise", 8));

            menuItemRepository.save(new MenuItem(restaurantId, "Vitello Tonnato",
                "Hauchdünn geschnittenes Kalbfleisch mit cremiger Thunfischsauce",
                new BigDecimal("12.90"), "Vorspeise", 10));

            menuItemRepository.save(new MenuItem(restaurantId, "Carpaccio",
                "Hauchdünnes rohes Rindfleisch mit Rucola und Parmesan",
                new BigDecimal("14.50"), "Vorspeise", 10));

            menuItemRepository.save(new MenuItem(restaurantId, "Antipasti Platte",
                "Auswahl an eingelegtem Gemüse, Schinken und italienischen Käsesorten",
                new BigDecimal("16.90"), "Vorspeise", 12));

            // === HAUPTGERICHTE (15 Stück) ===
            menuItemRepository.save(new MenuItem(restaurantId, "Spaghetti Carbonara",
                "Spaghetti mit Speck, Ei, Pecorino und schwarzem Pfeffer",
                new BigDecimal("14.90"), "Hauptgericht", 18));

            menuItemRepository.save(new MenuItem(restaurantId, "Spaghetti Bolognese",
                "Spaghetti mit hausgemachter Fleischsauce nach Bologneser Art",
                new BigDecimal("13.90"), "Hauptgericht", 20));

            menuItemRepository.save(new MenuItem(restaurantId, "Pizza Margherita",
                "Tomaten, Mozzarella und frisches Basilikum",
                new BigDecimal("12.50"), "Hauptgericht", 15));

            menuItemRepository.save(new MenuItem(restaurantId, "Pizza Salami",
                "Tomaten, Mozzarella und würzige Salami",
                new BigDecimal("14.50"), "Hauptgericht", 15));

            menuItemRepository.save(new MenuItem(restaurantId, "Pizza Quattro Formaggi",
                "Mozzarella, Gorgonzola, Parmesan und Fontina",
                new BigDecimal("15.90"), "Hauptgericht", 15));

            menuItemRepository.save(new MenuItem(restaurantId, "Lasagne",
                "Hausgemachte Lasagne mit Bolognese und Béchamelsauce",
                new BigDecimal("15.50"), "Hauptgericht", 25));

            menuItemRepository.save(new MenuItem(restaurantId, "Risotto ai Funghi",
                "Cremiges Risotto mit frischen Waldpilzen und Parmesan",
                new BigDecimal("16.90"), "Hauptgericht", 25));

            menuItemRepository.save(new MenuItem(restaurantId, "Ossobuco",
                "Geschmorte Kalbshaxe in Weißweinsauce mit Gremolata",
                new BigDecimal("24.90"), "Hauptgericht", 35));

            menuItemRepository.save(new MenuItem(restaurantId, "Saltimbocca",
                "Kalbsschnitzel mit Parmaschinken und Salbei in Weißweinsauce",
                new BigDecimal("22.90"), "Hauptgericht", 20));

            menuItemRepository.save(new MenuItem(restaurantId, "Scaloppine al Limone",
                "Kalbsschnitzel in Zitronen-Butter-Sauce",
                new BigDecimal("19.90"), "Hauptgericht", 18));

            menuItemRepository.save(new MenuItem(restaurantId, "Bistecca Fiorentina",
                "Gegrilltes T-Bone Steak nach Florentiner Art (ca. 500g)",
                new BigDecimal("32.90"), "Hauptgericht", 25));

            menuItemRepository.save(new MenuItem(restaurantId, "Gnocchi al Pesto",
                "Hausgemachte Kartoffelgnocchi mit frischem Basilikumpesto",
                new BigDecimal("14.50"), "Hauptgericht", 15));

            menuItemRepository.save(new MenuItem(restaurantId, "Penne Arrabiata",
                "Penne mit scharfer Tomaten-Knoblauch-Sauce",
                new BigDecimal("12.90"), "Hauptgericht", 15));

            menuItemRepository.save(new MenuItem(restaurantId, "Ravioli Ricotta e Spinaci",
                "Hausgemachte Ravioli gefüllt mit Ricotta und Spinat",
                new BigDecimal("15.90"), "Hauptgericht", 18));

            menuItemRepository.save(new MenuItem(restaurantId, "Tagliatelle al Tartufo",
                "Frische Bandnudeln mit schwarzem Trüffel und Parmesan",
                new BigDecimal("21.90"), "Hauptgericht", 18));

            // === DESSERTS (5 Stück) ===
            menuItemRepository.save(new MenuItem(restaurantId, "Tiramisu",
                "Klassisches italienisches Dessert mit Mascarpone und Espresso",
                new BigDecimal("7.50"), "Dessert", 5));

            menuItemRepository.save(new MenuItem(restaurantId, "Panna Cotta",
                "Cremiger Vanillepudding mit Beerensoße",
                new BigDecimal("6.50"), "Dessert", 5));

            menuItemRepository.save(new MenuItem(restaurantId, "Affogato",
                "Vanilleeis übergossen mit heißem Espresso",
                new BigDecimal("5.90"), "Dessert", 5));

            menuItemRepository.save(new MenuItem(restaurantId, "Cannoli Siciliani",
                "Knusprige Teigröllchen gefüllt mit süßer Ricottacreme",
                new BigDecimal("6.90"), "Dessert", 5));

            menuItemRepository.save(new MenuItem(restaurantId, "Torta della Nonna",
                "Toskanischer Oma-Kuchen mit Vanillecreme und Pinienkernen",
                new BigDecimal("7.90"), "Dessert", 5));

            // === GETRÄNKE (9 Stück) ===
            menuItemRepository.save(new MenuItem(restaurantId, "Espresso",
                "Starker italienischer Kaffee",
                new BigDecimal("2.80"), "Getränk", 3));

            menuItemRepository.save(new MenuItem(restaurantId, "Cappuccino",
                "Espresso mit aufgeschäumter Milch",
                new BigDecimal("3.50"), "Getränk", 5));

            menuItemRepository.save(new MenuItem(restaurantId, "Mineralwasser 0.5l",
                "Stilles oder prickelndes Mineralwasser",
                new BigDecimal("3.20"), "Getränk", 1));

            menuItemRepository.save(new MenuItem(restaurantId, "Coca Cola 0.33l",
                "Erfrischende Cola",
                new BigDecimal("3.50"), "Getränk", 1));

            menuItemRepository.save(new MenuItem(restaurantId, "Aperol Spritz",
                "Aperol mit Prosecco und Soda",
                new BigDecimal("8.50"), "Getränk", 5));

            menuItemRepository.save(new MenuItem(restaurantId, "Hauswein Rot 0.25l",
                "Ausgewählter italienischer Rotwein",
                new BigDecimal("5.90"), "Getränk", 2));

            menuItemRepository.save(new MenuItem(restaurantId, "Hauswein Weiß 0.25l",
                "Ausgewählter italienischer Weißwein",
                new BigDecimal("5.90"), "Getränk", 2));

            menuItemRepository.save(new MenuItem(restaurantId, "Limoncello",
                "Hausgemachter Zitronenlikör aus Sizilien",
                new BigDecimal("4.50"), "Getränk", 2));

            menuItemRepository.save(new MenuItem(restaurantId, "San Pellegrino 0.75l",
                "Italienisches Mineralwasser mit Kohlensäure",
                new BigDecimal("5.50"), "Getränk", 1));

            System.out.println("MenuService: 35 Menü-Items erfolgreich initialisiert!");
        }
    }

    // CRUD Operations
    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    public List<MenuItem> getMenuByRestaurant(Long restaurantId) {
        return menuItemRepository.findByRestaurantIdOrderByCategoryAscNameAsc(restaurantId);
    }

    public List<MenuItem> getAvailableMenuItems(Long restaurantId) {
        return menuItemRepository.findByRestaurantIdAndIsAvailable(restaurantId, true);
    }

    public List<MenuItem> getMenuItemsByCategory(Long restaurantId, String category) {
        return menuItemRepository.findByRestaurantIdAndCategory(restaurantId, category);
    }

    public Optional<MenuItem> getMenuItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    public MenuItem createMenuItem(MenuItem menuItem) {
        return menuItemRepository.save(menuItem);
    }

    public MenuItem updateMenuItem(Long id, MenuItem updatedItem) {
        return menuItemRepository.findById(id)
                .map(existingItem -> {
                    existingItem.setName(updatedItem.getName());
                    existingItem.setDescription(updatedItem.getDescription());
                    existingItem.setPrice(updatedItem.getPrice());
                    existingItem.setCategory(updatedItem.getCategory());
                    existingItem.setAvailable(updatedItem.isAvailable());
                    existingItem.setPreparationTimeMinutes(updatedItem.getPreparationTimeMinutes());
                    return menuItemRepository.save(existingItem);
                })
                .orElseThrow(() -> new RuntimeException("MenuItem nicht gefunden: " + id));
    }

    public void deleteMenuItem(Long id) {
        menuItemRepository.deleteById(id);
    }

    public MenuItem toggleAvailability(Long id) {
        return menuItemRepository.findById(id)
                .map(item -> {
                    item.setAvailable(!item.isAvailable());
                    return menuItemRepository.save(item);
                })
                .orElseThrow(() -> new RuntimeException("MenuItem nicht gefunden: " + id));
    }
}
