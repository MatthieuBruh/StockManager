package fi.haagahelia.stockmanager.repository.supplier.order;

import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, Long> {
    Optional<SupplierOrder> findById(Long id);
    Page<SupplierOrder> findAll(Specification<SupplierOrder> spec, Pageable pageable);
    List<SupplierOrder> findBySupplierId(Long id);
    Page<SupplierOrder> findBySupplierId(Long id, Specification<SupplierOrder> spec, Pageable pageable);
    List<SupplierOrder> findByDeliveryDate(LocalDate date);
    Page<SupplierOrder> findByDeliveryDate(LocalDate date, Specification<SupplierOrder> spec, Pageable pageable);
}
