package fi.haagahelia.stockmanager.repository.supplier;

import fi.haagahelia.stockmanager.model.supplier.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findById(Long id);
    Page<Supplier> findAll(Specification<Supplier> spec, Pageable pageable);
    Optional<Supplier> findByName(String name);
    Boolean existsByName(String name);
    Boolean existsByGeolocationId(Long id);
}
