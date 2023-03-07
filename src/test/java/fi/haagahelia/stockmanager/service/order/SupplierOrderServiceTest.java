package fi.haagahelia.stockmanager.service.order;

import fi.haagahelia.stockmanager.exception.OrderStateException;
import fi.haagahelia.stockmanager.exception.ProductStockException;
import fi.haagahelia.stockmanager.exception.UnknownOrderException;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLine;
import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Log4j2
public class SupplierOrderServiceTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @InjectMocks
    private SupplierOrderService supplierOrderService;

    private SupplierOrder supplierOrder;
    private Product productOne;
    private Product productTwo;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        EntityManager em = testEntityManager.getEntityManager();
        supplierOrderService.setEm(testEntityManager.getEntityManager());

        em.createQuery("DELETE SupplierOrderLine ").executeUpdate();
        em.createQuery("DELETE SupplierOrder").executeUpdate();
        em.createQuery("DELETE Product").executeUpdate();
        em.createQuery("DELETE Supplier").executeUpdate();
        em.createQuery("DELETE Category").executeUpdate();
        em.createQuery("DELETE Brand").executeUpdate();
        log.info("SUPPLIER ORDER MANAGER TEST - INIT - DATABASE CLEARED.");

        Brand brand = new Brand("Samsung");
        em.persist(brand);
        log.info("SUPPLIER ORDER MANAGER TEST - INIT - New brand saved: {}.", brand);

        Category category = new Category("SSD", "This is for SSD storages.");
        em.persist(category);
        log.info("SUPPLIER ORDER MANAGER TEST - INIT - New category saved: {}.", category);

        Supplier supplier = new Supplier("Samsung - Supplier", "supplier@samsung.com", "03443242", null);
        em.persist(supplier);
        log.info("SUPPLIER ORDER MANAGER TEST - INIT - New supplier saved: {}.", supplier);

        productOne = new Product("T7 TITAN", "This is a T7 1TB.", 100.0, 120.50, 10, 20, 30, brand, category, supplier);
        em.persist(productOne);
        log.info("SUPPLIER ORDER MANAGER TEST - INIT - New product saved: {}.", productOne);

        productTwo = new Product("870 EVO", "This is a 870 EVO.", 130.30, 160.20, 20, 15, 25, brand, category, supplier);
        em.persist(productTwo);
        log.info("SUPPLIER ORDER MANAGER TEST - INIT - New product saved: {}.", productTwo);

        supplierOrder = new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier);
        em.persist(supplierOrder);
        log.info("SUPPLIER ORDER MANAGER TEST - INIT - New customer order saved: {}.", supplierOrder);

        SupplierOrderLine supplierOrderLineOne = new SupplierOrderLine(2, 100.0, supplierOrder, productOne);
        em.persist(supplierOrderLineOne);
        log.info("SUPPLIER ORDER MANAGER TEST - INIT - New supplier order line saved: {}.", supplierOrderLineOne);

        SupplierOrderLine supplierOrderLineTwo = new SupplierOrderLine(3, 130.30, supplierOrder, productTwo);
        em.persist(supplierOrderLineTwo);
        log.info("SUPPLIER ORDER MANAGER TEST - INIT - New supplier order line saved: {}.", supplierOrderLineTwo);
        em.getTransaction().commit();
    }

    @Test
    public void sendOrderById() throws OrderStateException, UnknownOrderException {
        // Execution
        SupplierOrder foundOrder = supplierOrderService.sendOrderById(supplierOrder.getId());
        log.info("SUPPLIER ORDER MANAGER TEST - SEND ORDER BY ID - Execution done.");
        // Verification
        assertNotNull(foundOrder);
        assertNotNull(foundOrder.getId()); assertEquals(supplierOrder.getId(), foundOrder.getId());
        assertNotNull(foundOrder.getDate()); assertEquals(supplierOrder.getDate(), foundOrder.getDate());
        assertNotNull(foundOrder.getDeliveryDate()); assertEquals(supplierOrder.getDeliveryDate(), foundOrder.getDeliveryDate());
        assertNotNull(foundOrder.getReceived()); assertEquals(supplierOrder.getReceived(), foundOrder.getReceived());
        assertNotNull(foundOrder.getSupplier()); assertEquals(supplierOrder.getSupplier(), foundOrder.getSupplier());
        assertNotNull(foundOrder.getOrderIsSent()); assertEquals(supplierOrder.getOrderIsSent(), foundOrder.getOrderIsSent());
        assertEquals(true, foundOrder.getOrderIsSent());
        log.info("SUPPLIER ORDER MANAGER TEST - SEND ORDER BY ID - Verifications done.");
    }

    @Test
    public void sendOrderByIdThrowsOrderStateException() throws OrderStateException, UnknownOrderException {
        // Execution
        OrderStateException exception = assertThrows(OrderStateException.class, () -> {
            supplierOrderService.sendOrderById(supplierOrder.getId());
            supplierOrderService.sendOrderById(supplierOrder.getId());
        });
        log.info("SUPPLIER ORDER MANAGER TEST - SEND ORDER BY ID ORDER STATE EXCEPTION - Execution done.");
        // Verification
        assertNotNull(exception);
        assertEquals(OrderStateException.class, exception.getClass());
        log.info("SUPPLIER ORDER MANAGER TEST - SEND ORDER BY ID ORDER STATE EXCEPTION - Verifications done.");
    }

    @Test
    public void sendOrderByIdThrowsWrongIdException() throws OrderStateException, UnknownOrderException {
        // Execution
        UnknownOrderException exception = assertThrows(UnknownOrderException.class, () -> {
            supplierOrderService.sendOrderById(99999L);
        });
        log.info("SUPPLIER ORDER MANAGER TEST - SEND ORDER BY ID WRONG ID EXCEPTION - Execution done.");
        // Verification
        assertNotNull(exception);
        assertEquals(UnknownOrderException.class, exception.getClass());
        log.info("SUPPLIER ORDER MANAGER TEST - SEND ORDER BY ID WRONG ID EXCEPTION - Verifications done.");
    }

    @Test
    public void receiveOrderById() throws OrderStateException, UnknownOrderException, ProductStockException {
        // Execution
        supplierOrderService.sendOrderById(supplierOrder.getId());
        SupplierOrder receivedOrder = supplierOrderService.receiveOrderById(this.supplierOrder.getId());
        log.info("SUPPLIER ORDER MANAGER TEST - RECEIVE ORDER BY ID - Execution done.");
        // Verification
        assertNotNull(receivedOrder);
        assertTrue(receivedOrder.getReceived());
        assertEquals(70, productOne.getStock());
        assertEquals(95, productTwo.getStock());
        log.info("SUPPLIER ORDER MANAGER TEST - RECEIVE ORDER BY ID - Verifications done.");
    }

    @Test
    public void receiveOrderByIdOrderState() throws OrderStateException, UnknownOrderException, ProductStockException {
        // Execution
        OrderStateException exception = assertThrows(OrderStateException.class, () -> {
            supplierOrderService.receiveOrderById(supplierOrder.getId());
            supplierOrderService.receiveOrderById(supplierOrder.getId());
        });
        log.info("SUPPLIER ORDER MANAGER TEST - RECEIVE ORDER BY ID ORDER STATE - Execution done.");
        // Verification
        assertNotNull(exception);
        assertEquals(OrderStateException.class, exception.getClass());
        log.info("SUPPLIER ORDER MANAGER TEST - RECEIVE ORDER BY ID ORDER STATE - Verifications done.");
    }

    @Test
    public void receiveOrderByIdWrongOrder() throws OrderStateException, UnknownOrderException, ProductStockException {
        // Execution
        UnknownOrderException exception = assertThrows(UnknownOrderException.class, () -> {
            supplierOrderService.receiveOrderById(999L);
        });
        log.info("SUPPLIER ORDER MANAGER TEST - RECEIVE ORDER BY ID WRONG ORDER - Execution done.");
        // Verification
        assertNotNull(exception);
        assertEquals(UnknownOrderException.class, exception.getClass());
        log.info("SUPPLIER ORDER MANAGER TEST - RECEIVE ORDER BY ID WRONG ORDER - Verifications done.");
    }

    @Test
    public void cancelReceiveOrder() throws OrderStateException, UnknownOrderException, ProductStockException {
        // Execution
        supplierOrderService.sendOrderById(supplierOrder.getId());
        supplierOrderService.receiveOrderById(supplierOrder.getId());
        SupplierOrder receivedOrder = supplierOrderService.cancelReceiveOrder(supplierOrder.getId());
        log.info("SUPPLIER ORDER MANAGER TEST - CANCEL RECEIVE ORDER BY ID - Execution done.");
        // Verification
        assertNotNull(receivedOrder);
        assertFalse(receivedOrder.getReceived());
        assertNotEquals(70, productOne.getStock()); assertEquals(10, productOne.getStock());
        assertNotEquals(95, productTwo.getStock()); assertEquals(20, productTwo.getStock());
        log.info("SUPPLIER ORDER MANAGER TEST - CANCEL RECEIVE ORDER BY ID - Verifications done.");
    }

    @Test
    public void cancelReceiveOrderWrongState() throws OrderStateException, UnknownOrderException, ProductStockException {
        // Execution
        supplierOrderService.sendOrderById(supplierOrder.getId());
        supplierOrderService.receiveOrderById(supplierOrder.getId());
        OrderStateException exception = assertThrows(OrderStateException.class, () -> {
            supplierOrderService.cancelReceiveOrder(supplierOrder.getId());
            supplierOrderService.cancelReceiveOrder(supplierOrder.getId());
        });
        log.info("SUPPLIER ORDER MANAGER TEST - CANCEL RECEIVE ORDER BY ID WRONG STATE - Execution done.");
        // Verification
        assertNotNull(exception);
        assertEquals(OrderStateException.class, exception.getClass());
        log.info("SUPPLIER ORDER MANAGER TEST - CANCEL RECEIVE ORDER BY ID WRONG STATE - Verifications done.");
    }

    @Test
    public void cancelReceiveOrderWrongOrderId() throws OrderStateException, UnknownOrderException, ProductStockException {
        // Execution
        supplierOrderService.sendOrderById(supplierOrder.getId());
        supplierOrderService.receiveOrderById(supplierOrder.getId());
        UnknownOrderException exception = assertThrows(UnknownOrderException.class, () -> {
            supplierOrderService.cancelReceiveOrder(999L);
        });
        log.info("SUPPLIER ORDER MANAGER TEST - CANCEL RECEIVE ORDER BY ID WRONG ORDER ID - Execution done.");
        // Verification
        assertNotNull(exception);
        assertEquals(UnknownOrderException.class, exception.getClass());
        log.info("SUPPLIER ORDER MANAGER TEST - CANCEL RECEIVE ORDER BY ID WRONG ORDER ID - Verifications done.");
    }
}