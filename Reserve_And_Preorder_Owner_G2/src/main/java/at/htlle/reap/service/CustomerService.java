package at.htlle.reap.service;

import at.htlle.reap.model.Customer;
import at.htlle.reap.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Register a new customer with encrypted password
     * @param name Customer name
     * @param email Customer email (must be unique)
     * @param password Plain text password
     * @param phoneNumber Phone number (optional)
     * @return Created customer
     * @throws RuntimeException if email already exists
     */
    @Transactional
    public Customer registerCustomer(String name, String email, String password, String phoneNumber) {
        // Check if email already exists
        if (customerRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered: " + email);
        }

        // Hash password with BCrypt
        String passwordHash = passwordEncoder.encode(password);

        // Create new customer
        Customer customer = new Customer(name, email, passwordHash, phoneNumber);

        Customer saved = customerRepository.save(customer);
        System.out.println("Registered new customer: " + saved.getId() + " - " + saved.getEmail());

        return saved;
    }

    /**
     * Authenticate customer with email and password
     * @param email Customer email
     * @param password Plain text password
     * @return Customer if authentication successful
     * @throws RuntimeException if authentication fails
     */
    public Customer authenticateCustomer(String email, String password) {
        Optional<Customer> customerOpt = customerRepository.findByEmail(email);

        if (customerOpt.isEmpty()) {
            throw new RuntimeException("Invalid email or password");
        }

        Customer customer = customerOpt.get();

        // Verify password with BCrypt
        if (!passwordEncoder.matches(password, customer.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        System.out.println("Customer authenticated: " + customer.getId() + " - " + customer.getEmail());
        return customer;
    }

    /**
     * Get customer by ID
     * @param id Customer ID
     * @return Customer
     * @throws RuntimeException if customer not found
     */
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    /**
     * Get customer by email
     * @param email Customer email
     * @return Optional containing the customer if found
     */
    public Optional<Customer> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    /**
     * Update customer information
     * @param id Customer ID
     * @param name New name (optional)
     * @param phoneNumber New phone number (optional)
     * @return Updated customer
     */
    @Transactional
    public Customer updateCustomer(Long id, String name, String phoneNumber) {
        Customer customer = getCustomerById(id);

        if (name != null && !name.trim().isEmpty()) {
            customer.setName(name);
        }

        if (phoneNumber != null) {
            customer.setPhoneNumber(phoneNumber);
        }

        customer.setUpdatedAt(LocalDateTime.now());

        Customer updated = customerRepository.save(customer);
        System.out.println("Updated customer: " + updated.getId());

        return updated;
    }

    /**
     * Update customer password
     * @param id Customer ID
     * @param oldPassword Current password (for verification)
     * @param newPassword New password
     * @return Updated customer
     * @throws RuntimeException if old password doesn't match
     */
    @Transactional
    public Customer updatePassword(Long id, String oldPassword, String newPassword) {
        Customer customer = getCustomerById(id);

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, customer.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Hash and set new password
        String newPasswordHash = passwordEncoder.encode(newPassword);
        customer.setPasswordHash(newPasswordHash);
        customer.setUpdatedAt(LocalDateTime.now());

        Customer updated = customerRepository.save(customer);
        System.out.println("Password updated for customer: " + updated.getId());

        return updated;
    }

    /**
     * Delete customer (for GDPR compliance)
     * @param id Customer ID
     */
    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        customerRepository.deleteById(id);
        System.out.println("Deleted customer: " + id + " - " + customer.getEmail());
    }

    /**
     * Check if customer exists by email
     * @param email Email address
     * @return true if exists, false otherwise
     */
    public boolean customerExists(String email) {
        return customerRepository.existsByEmail(email);
    }
}
