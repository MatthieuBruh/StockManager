package fi.haagahelia.stockmanager.repository.customer.order;

import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findById(Long id);
    Page<CustomerOrder> findAll(Specification<CustomerOrder> spec, Pageable pageable);

    @Query(value = "SELECT c FROM CustomerOrder c WHERE c.customer.id = ?1")
    Page<CustomerOrder> findByCustomerId(Long id, Specification<CustomerOrder> spec, Pageable pageable);

    @Query(value = "SELECT c FROM CustomerOrder c WHERE c.deliveryDate = ?1")
    Page<CustomerOrder> findByDeliveryDate(LocalDate date, Specification<CustomerOrder> spec, Pageable pageable);

    @Query(value = "SELECT o.isSent FROM CustomerOrder o WHERE o.id = ?1")
    Boolean isCustomerOrderSentByOrderId(Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE CustomerOrder c SET c.customer = null WHERE c.customer.id = ?1")
    void removeRelatedCustomer(Long customerId);
}
