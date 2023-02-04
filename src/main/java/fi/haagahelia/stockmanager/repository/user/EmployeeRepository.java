package fi.haagahelia.stockmanager.repository.user;

import fi.haagahelia.stockmanager.model.user.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findById(Long id);
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);
    Optional<Employee> findByUsername(String username);
}
