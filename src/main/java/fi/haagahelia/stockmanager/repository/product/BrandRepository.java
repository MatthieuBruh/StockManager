package fi.haagahelia.stockmanager.repository.product;

import fi.haagahelia.stockmanager.model.product.brand.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findById(Long id);
    Page<Brand> findAll(Specification<Brand> spec, Pageable pageable);
    Boolean existsByName(String name);
}
