package fi.haagahelia.stockmanager.repository.customer.order;

import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findById(Long id);
    List<CustomerOrder> findByCustomerId(Long id);

    @Query(value = "SELECT o.isSent FROM CustomerOrder o WHERE o.id = ?1")
    Boolean getCustomerOrderSentByCustomerId(Long id);
}
