package fi.haagahelia.stockmanager.repository.product;

import fi.haagahelia.stockmanager.model.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findById(Long id);
    Boolean existsByNameAndSupplierId(String name, Long id);
    @Query(value = "SELECT p FROM Product p WHERE p.stock <= p .minStock")
    List<Product> findByStockIsLessThanMinStock();

    Boolean existsByBrandId(Long id);
    Boolean existsByCategoryId(Long id);
    Boolean existsBySupplierId(Long id);
}
