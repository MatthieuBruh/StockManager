package fi.haagahelia.stockmanager.service.order;

import fi.haagahelia.stockmanager.exception.EmptyOrderException;
import fi.haagahelia.stockmanager.exception.OrderStateException;
import fi.haagahelia.stockmanager.exception.ProductStockException;
import fi.haagahelia.stockmanager.exception.UnknownOrderException;
import fi.haagahelia.stockmanager.model.customer.Customer;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrderLine;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Log4j2
public class CustomerOrderServiceTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @InjectMocks
    private CustomerOrderService orderService;

    private CustomerOrder customerOrder;
    private Product productOne;
    private Product productTwo;
    private CustomerOrderLine customerOrderLineOne;
    private CustomerOrderLine customerOrderLineTwo;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        EntityManager em = testEntityManager.getEntityManager();
        orderService.setEm(testEntityManager.getEntityManager());

        em.createQuery("DELETE CustomerOrderLine").executeUpdate();
        em.createQuery("DELETE CustomerOrder").executeUpdate();
        em.createQuery("DELETE Customer").executeUpdate();
        em.createQuery("DELETE Employee").executeUpdate();
        em.createQuery("DELETE Role").executeUpdate();
        em.createQuery("DELETE Category").executeUpdate();
        em.createQuery("DELETE Brand").executeUpdate();
        log.info("CUSTOMER ORDER LINES TEST - INIT - DATABASE CLEARED.");

        Brand brand = new Brand("Ovomaltine");
        em.persist(brand);
        log.info("CUSTOMER ORDER SERVICE TEST - INIT - New brand saved: {}.", brand);

        Category category = new Category("Chocolate", "This is for chocolate products");
        em.persist(category);
        log.info("CUSTOMER ORDER SERVICE TEST - INIT - New category saved: {}.", category);

        Role role = new Role("ROLE_TESTING", "ROLE");
        em.persist(role);
        log.info("CUSTOMER ORDER SERVICE TEST - INIT - New role saved: {}.", role);

        Employee employee = new Employee("testing@haaga-helia.fi", "test", "John", "Doe",
                new BCryptPasswordEncoder().encode("AAAA"), false, true);
        employee.addRole(role);
        em.persist(employee);
        log.info("CUSTOMER ORDER SERVICE TEST - INIT - New employee saved: {}.", employee);

        Customer customer = new Customer("Jack", "Daniel", "jack@daniel.fi", null);
        em.persist(customer);
        log.info("CUSTOMER ORDER SERVICE TEST - INIT - New customer saved: {}.", customer);

        customerOrder = new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer);
        em.persist(customerOrder);
        log.info("CUSTOMER ORDER SERVICE TEST - INIT - New customer order saved: {}.", customerOrder);

        Supplier supplier = new Supplier("Alco", "alco@alco.fi", "03443242", null);
        em.persist(supplier);
        log.info("CUSTOMER ORDER SERVICE TEST - INIT - New supplier saved: {}.", supplier);

        productOne = new Product("Milk chocolate", "This is a milk chocolate",
                2.30, 3.50, 40, 20, 30, brand, category, supplier);
        em.persist(productOne);
        log.info("CUSTOMER ORDER SERVICE TEST - INIT - New product saved: {}.", productOne);

        productTwo = new Product("White chocolate", "This is a white chocolate",
                3.30, 4.20, 60, 15, 25, brand, category, supplier);
        em.persist(productTwo);
        log.info("CUSTOMER ORDER SERVICE TEST - INIT - New product saved: {}.", productTwo);

        customerOrderLineOne = new CustomerOrderLine(20, 3.50, customerOrder, productOne);
        em.persist(customerOrderLineOne);
        log.info("CUSTOMER ORDER SERVICE TEST - INIT - New customer order line saved: {}.", customerOrderLineOne);

        customerOrderLineTwo = new CustomerOrderLine(5, 4.20, customerOrder, productTwo);
        em.persist(customerOrderLineTwo);
        log.info("CUSTOMER ORDER SERVICE TEST - INIT - New customer order line saved: {}.", customerOrderLineTwo);
    }

    @Test
    public void customerOrderShipment() throws OrderStateException, UnknownOrderException, ProductStockException, EmptyOrderException {
        // Execution
        CustomerOrder shippedOrder = orderService.customerOrderShipment(customerOrder.getId());
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT - Execution done.");
        // Verification
        assertNotNull(shippedOrder);
        assertNotNull(shippedOrder.getId()); assertEquals(customerOrder.getId(), shippedOrder.getId());
        assertNotNull(shippedOrder.getDate()); assertEquals(customerOrder.getDate(), shippedOrder.getDate());
        assertNotNull(shippedOrder.getDeliveryDate()); assertEquals(customerOrder.getDeliveryDate(), shippedOrder.getDeliveryDate());
        assertNotNull(shippedOrder.getSent()); assertEquals(customerOrder.getSent(), shippedOrder.getSent());
        assertNotNull(shippedOrder.getEmployee()); assertEquals(customerOrder.getEmployee(), shippedOrder.getEmployee());
        assertNotNull(shippedOrder.getCustomer()); assertEquals(customerOrder.getCustomer(), shippedOrder.getCustomer());
        assertTrue(customerOrder.getSent());
        assertEquals(20, productOne.getStock());
        assertEquals(55, productTwo.getStock());
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT - Verifications done.");
    }

    @Test
    public void customerOrderShipmentOrderState() throws OrderStateException, UnknownOrderException, ProductStockException {
        // Execution
        OrderStateException exception = assertThrows(OrderStateException.class, () -> {
            orderService.customerOrderShipment(customerOrder.getId());
            orderService.customerOrderShipment(customerOrder.getId());
        });
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT ORDER STATE EXCEPTION - Execution done.");
        // Verification
        assertNotNull(exception);
        assertEquals(OrderStateException.class, exception.getClass());
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT ORDER STATE EXCEPTION - Verifications done.");
    }

    @Test
    public void customerOrderShipmentWrongOrderId() throws OrderStateException, UnknownOrderException, ProductStockException {
        // Execution
        UnknownOrderException exception = assertThrows(UnknownOrderException.class, () -> {
            orderService.customerOrderShipment(99L);
        });
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT WRONG ORDER ID EXCEPTION - Execution done.");
        // Verification
        assertNotNull(exception);
        assertEquals(UnknownOrderException.class, exception.getClass());
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT WRONG ORDER ID EXCEPTION - Verifications done.");
    }

    @Test
    public void customerOrderShipmentNotEnoughStock() throws OrderStateException, UnknownOrderException, ProductStockException {
        // Execution
        EntityManager em = testEntityManager.getEntityManager();
        customerOrderLineOne.setQuantity(50);
        em.persist(customerOrderLineOne);
        ProductStockException exception = assertThrows(ProductStockException.class, () -> {
            orderService.customerOrderShipment(customerOrder.getId());
        });
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT NOT ENOUGH STOCK EXCEPTION - Execution done.");
        // Verification
        assertNotNull(exception);
        assertEquals(ProductStockException.class, exception.getClass());
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT NOT ENOUGH STOCK EXCEPTION - Verifications done.");
    }

    @Test
    public void customerOrderShipmentCancel() throws OrderStateException, UnknownOrderException, ProductStockException, EmptyOrderException {
        // Execution
        orderService.customerOrderShipment(customerOrder.getId());
        CustomerOrder shippedOrder = orderService.customerOrderShipmentCancel(customerOrder.getId());
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT CANCEL - Execution done.");
        // Verification
        assertNotNull(shippedOrder);
        assertNotNull(shippedOrder.getId()); assertEquals(customerOrder.getId(), shippedOrder.getId());
        assertNotNull(shippedOrder.getDate()); assertEquals(customerOrder.getDate(), shippedOrder.getDate());
        assertNotNull(shippedOrder.getDeliveryDate()); assertEquals(customerOrder.getDeliveryDate(), shippedOrder.getDeliveryDate());
        assertNotNull(shippedOrder.getSent()); assertEquals(customerOrder.getSent(), shippedOrder.getSent());
        assertNotNull(shippedOrder.getEmployee()); assertEquals(customerOrder.getEmployee(), shippedOrder.getEmployee());
        assertNotNull(shippedOrder.getCustomer()); assertEquals(customerOrder.getCustomer(), shippedOrder.getCustomer());
        assertFalse(customerOrder.getSent());
        assertEquals(40, productOne.getStock());
        assertEquals(60, productTwo.getStock());
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT CANCEL - Verifications done.");
    }

    @Test
    public void customerOrderShipmentCancelNotSentCancelled() throws OrderStateException, UnknownOrderException, ProductStockException {
        // Execution
        OrderStateException exception = assertThrows(OrderStateException.class, () -> {
            orderService.customerOrderShipmentCancel(customerOrder.getId());
        });
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT CANCEL ORDER STATE EXCEPTION - Execution done.");
        // Verification
        assertNotNull(exception);
        assertEquals(OrderStateException.class, exception.getClass());
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT CANCEL ORDER STATE EXCEPTION - Verifications done.");
    }

    @Test
    public void customerOrderShipmentCancelAlreadyCancelled() throws OrderStateException, UnknownOrderException, ProductStockException {
        // Execution
        OrderStateException exception = assertThrows(OrderStateException.class, () -> {
            orderService.customerOrderShipment(customerOrder.getId());
            orderService.customerOrderShipmentCancel(customerOrder.getId());
            orderService.customerOrderShipmentCancel(customerOrder.getId());
        });
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT CANCEL ORDER STATE EXCEPTION - Execution done.");
        // Verification
        assertNotNull(exception);
        assertEquals(OrderStateException.class, exception.getClass());
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT CANCEL ORDER STATE EXCEPTION - Verifications done.");
    }

    @Test
    public void customerOrderShipmentCancelWrongOrderId() throws OrderStateException, UnknownOrderException, ProductStockException {
        // Execution
        UnknownOrderException exception = assertThrows(UnknownOrderException.class, () -> {
            orderService.customerOrderShipmentCancel(99L);
        });
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT CANCEL WRONG ORDER ID EXCEPTION - Execution done.");
        // Verification
        assertNotNull(exception);
        assertEquals(UnknownOrderException.class, exception.getClass());
        log.info("CUSTOMER ORDER SERVICE TEST - CUSTOMER ORDER SHIPMENT CANCEL WRONG ORDER ID EXCEPTION - Verifications done.");
    }
}