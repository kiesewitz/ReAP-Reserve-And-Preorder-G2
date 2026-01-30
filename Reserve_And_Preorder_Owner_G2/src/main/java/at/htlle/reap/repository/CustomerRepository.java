package at.htlle.reap.repository;

import at.htlle.reap.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Find customer by email address
     * @param email Email address
     * @return Optional containing the customer if found
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Check if a customer with the given email exists
     * @param email Email address
     * @return true if customer exists, false otherwise
     */
    boolean existsByEmail(String email);
}
