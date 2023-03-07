package fi.haagahelia.stockmanager.repository.customer.order;

import fi.haagahelia.stockmanager.model.customer.Customer;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrderLine;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLine;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Log4j2
public class CustomerOrderLineRepositoryTest {

    @Autowired
    private CustomerOrderLineRepository lineRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private CustomerOrder customerOrder;
    private Product productOne;
    private Product productTwo;
    private CustomerOrderLine customerOrderLineOne;
    private CustomerOrderLine customerOrderLineTwo;

    @BeforeEach
    public void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        em.createQuery("DELETE CustomerOrderLine").executeUpdate();
        em.createQuery("DELETE CustomerOrder").executeUpdate();
        em.createQuery("DELETE Customer").executeUpdate();
        em.createQuery("DELETE Employee").executeUpdate();
        em.createQuery("DELETE Role").executeUpdate();
        em.createQuery("DELETE Category").executeUpdate();
        em.createQuery("DELETE Brand").executeUpdate();
        log.info("CUSTOMER ORDER LINES TEST - INIT - DATABASE CLEARED.");

        Brand brand = new Brand("Cailler");
        em.persist(brand);
        log.info("CUSTOMER ORDER LINES TEST - INIT - New brand saved: {}.", brand);

        Category category = new Category("Chocolate", "This is for chocolate products");
        em.persist(category);
        log.info("CUSTOMER ORDER LINES TEST - INIT - New category saved: {}.", category);

        Role role = new Role("ROLE_TESTING", "ROLE");
        em.persist(role);
        log.info("CUSTOMER ORDER LINES TEST - INIT - New role saved: {}.", role);

        Employee employee = new Employee("testing@haaga-helia.fi", "test", "John", "Doe", new BCryptPasswordEncoder().encode("AAAA"), false, false);
        employee.addRole(role);
        em.persist(employee);
        log.info("CUSTOMER ORDER LINES TEST - INIT - New employee saved: {}.", employee);

        Customer customer = new Customer("Jack", "Daniel", "jack@daniel.fi", null);
        em.persist(customer);
        log.info("CUSTOMER ORDER LINES TEST - INIT - New customer saved: {}.", customer);

        customerOrder = new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer);
        em.persist(customerOrder);
        log.info("CUSTOMER ORDER LINES TEST - INIT - New customer order saved: {}.", customerOrder);

        Supplier supplier = new Supplier("Alco", "alco@alco.fi", "03443242", null);
        em.persist(supplier);
        log.info("CUSTOMER ORDER LINES TEST - INIT - New supplier saved: {}.", supplier);

        productOne = new Product("Milk chocolate", "This is a milk chocolate", 2.30, 3.50, 40, 20, 30, brand, category, supplier);
        em.persist(productOne);
        log.info("CUSTOMER ORDER LINES TEST - INIT - New product saved: {}.", productOne);

        productTwo = new Product("White chocolate", "This is a white chocolate", 3.30, 4.20, 60, 15, 25, brand, category, supplier);
        em.persist(productTwo);
        log.info("CUSTOMER ORDER LINES TEST - INIT - New product saved: {}.", productTwo);

        customerOrderLineOne = new CustomerOrderLine(20, 3.50, customerOrder, productOne);
        em.persist(customerOrderLineOne);
        log.info("CUSTOMER ORDER LINES TEST - INIT - New customer order line saved: {}.", customerOrderLineOne);
        customerOrderLineTwo = new CustomerOrderLine(5, 4.20, customerOrder, productTwo);
        em.persist(customerOrderLineTwo);
        log.info("CUSTOMER ORDER LINES TEST - INIT - New customer order line saved: {}.", customerOrderLineTwo);
    }

    @Test
    public void findByCustomerOrderId() {
        // Execution
        Page<CustomerOrderLine> lines = lineRepository.findByCustomerOrderId(customerOrder.getId(), PageRequest.of(0, 10));
        log.info("CUSTOMER ORDER LINES TEST - FIND BY CUSTOMER ORDER ID - EXECUTION DONE.");
        // Verification
        assertNotNull(lines);
        assertFalse(lines.isEmpty());
        assertEquals(2, lines.getTotalElements());
        assertTrue(lines.getContent().contains(customerOrderLineOne));
        assertTrue(lines.getContent().contains(customerOrderLineTwo));
        log.info("CUSTOMER ORDER LINES TEST - FIND BY CUSTOMER ORDER ID - VERIFICATIONS DONE.");
    }

    @Test
    public void notFoundByCustomerOrderId() {
        // Execution
        Page<CustomerOrderLine> lines = lineRepository.findByCustomerOrderId(999L, PageRequest.of(0, 10));
        log.info("CUSTOMER ORDER LINES TEST - NOT FIND BY CUSTOMER ORDER ID - EXECUTION DONE.");
        // Verification
        assertNotNull(lines);
        assertTrue(lines.isEmpty());
        log.info("CUSTOMER ORDER LINES TEST - NOT FIND BY CUSTOMER ORDER ID - VERIFICATIONS DONE.");
    }

    @Test
    public void findByCustomerOrderIdAndProductId() {
        // Execution
        Optional<CustomerOrderLine> line = lineRepository.findByCustomerOrderIdAndProductId(customerOrder.getId(), productOne.getId());
        log.info("CUSTOMER ORDER LINES TEST - FIND BY CUSTOMER ORDER ID AND PRODUCT ID - EXECUTION DONE.");
        // Verification
        assertTrue(line.isPresent());
        assertEquals(customerOrderLineOne, line.get());
        log.info("CUSTOMER ORDER LINES TEST - FIND BY CUSTOMER ORDER ID AND PRODUCT ID - VERIFICATIONS DONE.");
    }

    @Test
    public void notFoundByCustomerOrderIdAndProductId() {
        // Execution
        Optional<CustomerOrderLine> result = lineRepository.findByCustomerOrderIdAndProductId(999L, 888L);
        log.info("CUSTOMER ORDER LINES TEST - NOT FIND BY CUSTOMER ORDER ID AND PRODUCT ID - EXECUTION DONE.");
        // Verification
        assertNotNull(result);
        assertFalse(result.isPresent());
        log.info("CUSTOMER ORDER LINES TEST - NOT FIND BY CUSTOMER ORDER ID AND PRODUCT ID - VERIFICATIONS DONE.");
    }

    @Test
    public void existsByCustomerOrderIdAndProductId() {
        // Execution
        Boolean result = lineRepository.existsByCustomerOrderIdAndProductId(customerOrder.getId(), productTwo.getId());
        log.info("CUSTOMER ORDER LINES TEST - EXISTS BY CUSTOMER ORDER ID AND PRODUCT ID - EXECUTION DONE.");
        // Verification
        assertNotNull(result);
        assertTrue(result);
        log.info("CUSTOMER ORDER LINES TEST - EXISTS BY CUSTOMER ORDER ID AND PRODUCT ID - VERIFICATIONS DONE.");
    }

    @Test
    public void doesNotExistByCustomerOrderIdAndProductId() {
        // Execution
        Boolean result = lineRepository.existsByCustomerOrderIdAndProductId(999L, 888L);
        log.info("CUSTOMER ORDER LINES TEST - DOEST NOT EXIST BY CUSTOMER ORDER ID AND PRODUCT ID - EXECUTION DONE.");
        // Verification
        assertNotNull(result);
        assertFalse(result);
        log.info("CUSTOMER ORDER LINES TEST - DOEST NOT EXIST BY CUSTOMER ORDER ID AND PRODUCT ID - VERIFICATIONS DONE.");
    }

    @Test
    public void deleteByCustomerOrderIdAndProductId() {
        // Execution
        assertDoesNotThrow(() -> lineRepository.deleteByCustomerOrderIdAndProductId(customerOrder.getId(), productOne.getId()));
        // Verification
        Optional<CustomerOrderLine> line = lineRepository.findByCustomerOrderIdAndProductId(customerOrder.getId(), productOne.getId());
        assertFalse(line.isPresent());
    }

    @Test
    public void existsByProductId() {
        // Execution
        Boolean result = lineRepository.existsByProductId(productOne.getId());
        log.info("CUSTOMER ORDER LINES TEST - EXISTS BY PRODUCT ID - EXECUTION DONE.");
        // Verification
        assertNotNull(result);
        assertTrue(result);
        log.info("CUSTOMER ORDER LINES TEST - EXISTS BY PRODUCT ID - VERIFICATIONS DONE.");
    }

    @Test
    public void doesNotExistByProductId() {
        Boolean result = lineRepository.existsByProductId(9999L);
        log.info("CUSTOMER ORDER LINES TEST - EXISTS BY PRODUCT ID - EXECUTION DONE.");
        assertNotNull(result);
        assertFalse(result);
        log.info("CUSTOMER ORDER LINES TEST - EXISTS BY PRODUCT ID - VERIFICATIONS DONE.");
    }
}