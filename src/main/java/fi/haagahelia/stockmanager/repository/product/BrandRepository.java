package fi.haagahelia.stockmanager.repository.product;

import fi.haagahelia.stockmanager.model.product.brand.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findById(Long id);
    Boolean existsByName(String name);


}
