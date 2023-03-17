package fi.haagahelia.stockmanager.repository.customer.order;

import fi.haagahelia.stockmanager.model.customer.order.CustomerOrderLine;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerOrderLineRepository extends JpaRepository<CustomerOrderLine, Long> {
    Page<CustomerOrderLine> findByCustomerOrderId(Long id, Pageable pageable);
    Optional<CustomerOrderLine> findByCustomerOrderIdAndProductId(Long customerOrderId, Long productId);
    Boolean existsByCustomerOrderIdAndProductId(Long customerOrderId, Long productId);
    @Transactional
    void deleteByCustomerOrderIdAndProductId(Long customerOrderId, Long productId);
    Boolean existsByProductId(Long id);
}
