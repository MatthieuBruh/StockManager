package fi.haagahelia.stockmanager.repository.user;

import fi.haagahelia.stockmanager.model.user.Employee;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findById(Long id);
    Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);
    Optional<Employee> findByUsername(String username);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE Employee e SET e.isActive = false, e.isBlocked = true WHERE e.id = :employeeId")
    void deactivateEmployeeById(@Param("employeeId") Long id);
}
