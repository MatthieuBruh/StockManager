package fi.haagahelia.stockmanager.repository.customer.order;

import fi.haagahelia.stockmanager.model.customer.order.CustomerOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderLineRepository extends JpaRepository<CustomerOrderLine, Long> {
    List<CustomerOrderLine> findByCustomerOrderId(Long id);
    Optional<CustomerOrderLine> findByCustomerOrderIdAndProductId(Long customerOrderId, Long productId);
    Boolean existsByCustomerOrderIdAndProductId(Long customerOrderId, Long productId);
    void deleteByCustomerOrderIdAndProductId(Long customerOrderId, Long productId);
}
