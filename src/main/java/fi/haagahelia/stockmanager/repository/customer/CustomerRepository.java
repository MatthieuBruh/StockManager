package fi.haagahelia.stockmanager.repository.customer;

import fi.haagahelia.stockmanager.model.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findById(Long id);
    Optional<Customer> findByEmail(String email);
    Boolean existsByEmail(String email);
    Boolean existsByGeolocationId(Long id);
}
