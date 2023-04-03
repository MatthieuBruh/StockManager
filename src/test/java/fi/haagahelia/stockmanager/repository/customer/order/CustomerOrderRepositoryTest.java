package fi.haagahelia.stockmanager.repository.customer.order;

import fi.haagahelia.stockmanager.model.customer.Customer;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Log4j2
public class CustomerOrderRepositoryTest {

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private Customer customer;
    private CustomerOrder customerOrder;

    @BeforeEach
    void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        em.createQuery("DELETE CustomerOrder").executeUpdate();
        em.createQuery("DELETE Customer").executeUpdate();
        em.createQuery("DELETE Employee").executeUpdate();
        em.createQuery("DELETE Role").executeUpdate();
        log.info("CUSTOMER ORDER TEST - INIT - DATABASE CLEARED.");

        Role role = new Role("ROLE_TESTING", "ROLE");
        em.persist(role);
        log.info("CUSTOMER ORDER TEST - INIT - New role saved: {}.", role);

        Employee employee = new Employee("testing@haaga-helia.fi", "test", "John", "Doe", new BCryptPasswordEncoder().encode("AAAA"), false, false);
        employee.addRole(role);
        em.persist(employee);
        log.info("CUSTOMER ORDER TEST - INIT - New employee saved: {}.", employee);

        customer = new Customer("Jack", "Daniel", "jack@daniel.fi", null);
        em.persist(customer);
        log.info("CUSTOMER ORDER TEST - INIT - New customer saved: {}.", customer);

        customerOrder = new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer);
        em.persist(customerOrder);
        log.info("CUSTOMER ORDER TEST - INIT - New customer order saved: {}.", customerOrder);
    }

    @Test
    public void findById() {
        // Execution
        Optional<CustomerOrder> orderOptional = orderRepository.findById(customerOrder.getId());
        log.info("CUSTOMER ORDER TEST - FIND BY ID - EXECUTION DONE.");
        // Verification
        assertTrue(orderOptional.isPresent());
        CustomerOrder foundOrder = orderOptional.get();
        assertNotNull(foundOrder);
        assertNotNull(foundOrder); assertEquals(customerOrder, foundOrder);
        assertNotNull(foundOrder.getId()); assertEquals(customerOrder.getId(), foundOrder.getId());
        assertNotNull(foundOrder.getDate()); assertEquals(customerOrder.getDate(), foundOrder.getDate());
        assertNotNull(foundOrder.getDeliveryDate()); assertEquals(customerOrder.getDeliveryDate(), foundOrder.getDeliveryDate());
        assertNotNull(foundOrder.getSent()); assertEquals(customerOrder.getSent(), foundOrder.getSent());
        assertNotNull(foundOrder.getEmployee()); assertEquals(customerOrder.getEmployee(), foundOrder.getEmployee());
        assertNotNull(foundOrder.getCustomer()); assertEquals(customerOrder.getCustomer(), foundOrder.getCustomer());
        assertNotNull(foundOrder.getCustomerOrderLines()); assertEquals(customerOrder.getCustomerOrderLines(), foundOrder.getCustomerOrderLines());
        assertEquals(customerOrder.getCustomerOrderLines().size(), foundOrder.getCustomerOrderLines().size());
        log.info("CUSTOMER ORDER TEST - FIND BY ID - VERIFICATION DONE.");
    }

    @Test
    public void notFoundById() {
        // Execution
        Optional<CustomerOrder> orderOptional = orderRepository.findById(999L);
        log.info("CUSTOMER ORDER TEST - NOT FOUND BY ID - EXECUTION DONE.");
        // Verification
        assertFalse(orderOptional.isPresent());
    }

    @Test
    public void findAll() {
        // Execution
        Page<CustomerOrder> orders = orderRepository.findAll(PageRequest.of(0, 10));
        log.info("CUSTOMER ORDER TEST - FIND ALL - EXECUTION DONE.");
        // Verification
        assertNotNull(orders);
        assertEquals(1, orders.getTotalElements());
        assertEquals(customerOrder, orders.getContent().get(0));
        log.info("CUSTOMER ORDER TEST - FIND ALL - VERIFICATION DONE.");
    }

    @Test
    public void findByCustomerId() {
        // Execution
        Page<CustomerOrder> orders = orderRepository.findByCustomerId(customer.getId(), null, PageRequest.of(0, 10));
        log.info("CUSTOMER ORDER TEST - FIND BY CUSTOMER ID - EXECUTION DONE.");
        // Verification
        assertNotNull(orders);
        assertEquals(1, orders.getTotalElements());
        assertEquals(customerOrder, orders.getContent().get(0));
        log.info("CUSTOMER ORDER TEST - FIND BY CUSTOMER ID - VERIFICATION DONE.");
    }

    @Test
    public void notFoundByCustomerId() {
        // Execution
        Page<CustomerOrder> orders = orderRepository.findByCustomerId(999L, null, PageRequest.of(0, 10));
        log.info("CUSTOMER ORDER TEST - NOT FOUND BY CUSTOMER ID - EXECUTION DONE.");
        // Verification
        assertNotNull(orders);
        assertTrue(orders.isEmpty());
        log.info("CUSTOMER ORDER TEST - NOT FOUND BY CUSTOMER ID - VERIFICATION DONE.");
    }

    @Test
    public void findByDeliveryDate() {
        // Execution
        Page<CustomerOrder> orders = orderRepository.findByDeliveryDate(customerOrder.getDeliveryDate(), null, PageRequest.of(0, 10));
        log.info("CUSTOMER ORDER TEST - FIND BY DELIVERY DATE - EXECUTION DONE.");
        // Verification
        assertNotNull(orders);
        assertEquals(1, orders.getTotalElements());
        assertEquals(customerOrder, orders.getContent().get(0));
        log.info("CUSTOMER ORDER TEST - FIND BY DELIVERY DATE - VERIFICATION DONE.");
    }

    @Test
    public void notFoundByDeliveryDate() {
        // Execution
        Page<CustomerOrder> orders = orderRepository.findByDeliveryDate(LocalDate.now().minusDays(1), null, PageRequest.of(0, 10));
        log.info("CUSTOMER ORDER TEST - NOT FOUND BY DELIVERY DATE - EXECUTION DONE.");
        // Verification
        assertNotNull(orders);
        assertTrue(orders.isEmpty());
        log.info("CUSTOMER ORDER TEST - NOT FOUND BY DELIVERY DATE - VERIFICATION DONE.");
    }

    @Test
    public void isCustomerOrderSentByOrderId() {
        // Execution
        Boolean result = orderRepository.isCustomerOrderSentByOrderId(customerOrder.getId());
        log.info("CUSTOMER ORDER TEST - GET CUSTOMER ORDER SENT BY CUSTOMER ID - EXECUTION DONE.");
        // Verification
        assertNotNull(result);
        assertFalse(result);
        log.info("CUSTOMER ORDER TEST - GET CUSTOMER ORDER SENT BY CUSTOMER ID - VERIFICATION DONE.");
    }

    @Test
    public void removeRelatedCustomer() {
        // Execution
        orderRepository.delete(customerOrder);
        log.info("CUSTOMER ORDER TEST - REMOVE RELATED CUSTOMER - EXECUTION DONE.");
        // Verification
        assertFalse(orderRepository.findById(customerOrder.getId()).isPresent());
        Page<CustomerOrder> result = orderRepository.findByCustomerId(customer.getId(), null, PageRequest.of(0, 10));
        assertFalse(result.getContent().contains(customerOrder));
        log.info("CUSTOMER ORDER TEST - REMOVE RELATED CUSTOMER - VERIFICATION DONE.");
    }
}