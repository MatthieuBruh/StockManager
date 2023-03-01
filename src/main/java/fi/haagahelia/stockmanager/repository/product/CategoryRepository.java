package fi.haagahelia.stockmanager.repository.product;

import fi.haagahelia.stockmanager.model.product.category.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findById(Long id);
    Page<Category> findAll(Specification<Category> spec, Pageable pageable);
    Boolean existsByName(String name);
}
