package fi.haagahelia.stockmanager.repository.supplier.order;

import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLine;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierOrderLineRepository extends JpaRepository<SupplierOrderLine, Long> {
    Page<SupplierOrderLine> findBySupplierOrderId(Long id, Pageable pageable);
    Optional<SupplierOrderLine> findBySupplierOrderIdAndProductId(Long supplierOrderId, Long productId);
    @Transactional
    void deleteBySupplierOrderIdAndProductId(Long supplierOrderId, Long productId);
    Boolean existsByProductId(Long id);
    Boolean existsBySupplierOrderIdAndProductId(Long supplierId, Long productId);
}
