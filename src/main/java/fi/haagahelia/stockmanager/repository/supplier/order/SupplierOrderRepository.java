package fi.haagahelia.stockmanager.repository.supplier.order;

import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, Long> {
    Optional<SupplierOrder> findById(Long id);
    List<SupplierOrder> findBySupplierId(Long id);
}
