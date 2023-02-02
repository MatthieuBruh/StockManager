package fi.haagahelia.stockmanager.repository.supplier.order;

import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierOrderLineRepository extends JpaRepository<SupplierOrderLine, Long> {
    List<SupplierOrderLine> findBySupplierOrderId(Long id);
    Optional<SupplierOrderLine> findBySupplierOrderIdAndProductId(Long supplierOrderId, Long productId);
    void deleteBySupplierOrderIdAndProductId(Long supplierOrderId, Long productId);
}
