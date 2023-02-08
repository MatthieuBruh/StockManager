package fi.haagahelia.stockmanager.repository.customer.order;

import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findById(Long id);
    List<CustomerOrder> findByCustomerId(Long id);
    List<CustomerOrder> findByDeliveryDate(LocalDate date);

    @Query(value = "SELECT o.isSent FROM CustomerOrder o WHERE o.id = ?1")
    Boolean getCustomerOrderSentByCustomerId(Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE CustomerOrder c SET c.customer = null WHERE c.customer.id = ?1")
    void removeRelatedCustomer(Long customerId);
}
