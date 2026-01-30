package at.htlle.reap.controller;

import at.htlle.reap.model.Customer;
import at.htlle.reap.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Register a new customer
     * POST /api/customers/register
     *
     * Request body:
     * {
     *   "name": "John Doe",
     *   "email": "john@example.com",
     *   "password": "secret123",
     *   "phoneNumber": "+43 123 456 7890"  // optional
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String email = request.get("email");
            String password = request.get("password");
            String phoneNumber = request.get("phoneNumber");

            // Validation
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            // Email format validation (simple regex)
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
            }
            if (password == null || password.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
            }
            // Phone number validation (required)
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
            }
            // Phone number format validation (must contain digits and optionally + - or spaces)
            if (!phoneNumber.matches("^\\+?[0-9\\s-]{6,}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid phone number format"));
            }

            Customer customer = customerService.registerCustomer(name, email, password, phoneNumber);

            // Don't return password hash
            Map<String, Object> response = new HashMap<>();
            response.put("id", customer.getId());
            response.put("name", customer.getName());
            response.put("email", customer.getEmail());
            response.put("phoneNumber", customer.getPhoneNumber());
            response.put("createdAt", customer.getCreatedAt());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Login customer
     * POST /api/customers/login
     *
     * Request body:
     * {
     *   "email": "john@example.com",
     *   "password": "secret123"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            if (email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
            }

            Customer customer = customerService.authenticateCustomer(email, password);

            // Don't return password hash
            Map<String, Object> response = new HashMap<>();
            response.put("id", customer.getId());
            response.put("name", customer.getName());
            response.put("email", customer.getEmail());
            response.put("phoneNumber", customer.getPhoneNumber());
            response.put("createdAt", customer.getCreatedAt());
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    /**
     * Get customer by ID
     * GET /api/customers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCustomer(@PathVariable Long id) {
        try {
            Customer customer = customerService.getCustomerById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("id", customer.getId());
            response.put("name", customer.getName());
            response.put("email", customer.getEmail());
            response.put("phoneNumber", customer.getPhoneNumber());
            response.put("createdAt", customer.getCreatedAt());
            response.put("updatedAt", customer.getUpdatedAt());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update customer profile
     * PUT /api/customers/{id}
     *
     * Request body:
     * {
     *   "name": "John Doe Jr.",  // optional
     *   "phoneNumber": "+43 987 654 3210"  // optional
     * }
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCustomer(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String phoneNumber = request.get("phoneNumber");

            Customer customer = customerService.updateCustomer(id, name, phoneNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("id", customer.getId());
            response.put("name", customer.getName());
            response.put("email", customer.getEmail());
            response.put("phoneNumber", customer.getPhoneNumber());
            response.put("updatedAt", customer.getUpdatedAt());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update customer password
     * PUT /api/customers/{id}/password
     *
     * Request body:
     * {
     *   "oldPassword": "secret123",
     *   "newPassword": "newSecret456"
     * }
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<Map<String, Object>> updatePassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 6 characters"));
            }

            customerService.updatePassword(id, oldPassword, newPassword);

            return ResponseEntity.ok(Map.of("success", true, "message", "Password updated successfully"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete customer account (GDPR compliance)
     * DELETE /api/customers/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCustomer(@PathVariable Long id) {
        try {
            customerService.deleteCustomer(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Customer account deleted"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if email exists
     * GET /api/customers/check-email?email=john@example.com
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        boolean exists = customerService.customerExists(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}
