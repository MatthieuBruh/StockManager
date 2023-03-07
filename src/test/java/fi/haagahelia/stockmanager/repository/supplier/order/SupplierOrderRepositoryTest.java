package fi.haagahelia.stockmanager.repository.supplier.order;


import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
import org.springframework.data.jpa.domain.Specification;
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
public class SupplierOrderRepositoryTest {

    @Autowired
    private SupplierOrderRepository suppOrderRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @BeforeEach
    public void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        Query deleteSupplierOrder = em.createQuery("DELETE SupplierOrder ord"); deleteSupplierOrder.executeUpdate();
        Query deleteSupplier = em.createQuery("DELETE Supplier s"); deleteSupplier.executeUpdate();
        Query deleteProduct = em.createQuery("DELETE Product p"); deleteProduct.executeUpdate();
    }

    /**
     * This test is used to ensure that the supplier order repository can find a supplier order by its id.
     */
    @Test
    public void findById() {
        // Initialization
        EntityManager em = testEntityManager.getEntityManager();

        Supplier quinu = new Supplier("Quinu", "struss0@i2i.jp", null, null);
        em.persist(quinu);
        log.info("SUPPLIER ORDERS TEST - FIND BY ID - New supplier saved: " + quinu);
        SupplierOrder supplierOrder = new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3),
                false, false, quinu);
        em.persist(supplierOrder);
        log.info("SUPPLIER ORDERS TEST - FIND BY ID - New supplier order saved: " + supplierOrder);
        // Execution
        Optional<SupplierOrder> supplierOrderOptional = suppOrderRepository.findById(supplierOrder.getId());
        // Verification
        log.info("SUPPLIER ORDERS TEST - FIND BY ID - Supplier order verifications");
        assertTrue(supplierOrderOptional.isPresent());
        SupplierOrder suppOrdFound = supplierOrderOptional.get();
        assertNotNull(suppOrdFound);
        assertNotNull(suppOrdFound.getId()); assertEquals(supplierOrder.getId(), suppOrdFound.getId());
        assertNotNull(suppOrdFound.getDate()); assertEquals(supplierOrder.getDate(), suppOrdFound.getDate());
        assertNotNull(suppOrdFound.getDeliveryDate()); assertEquals(supplierOrder.getDeliveryDate(), suppOrdFound.getDeliveryDate());
        assertNotNull(suppOrdFound.getOrderIsSent()); assertEquals(supplierOrder.getOrderIsSent(), suppOrdFound.getOrderIsSent());
        assertNotNull(suppOrdFound.getReceived()); assertEquals(supplierOrder.getReceived(), suppOrdFound.getReceived());
        assertNotNull(suppOrdFound.getSupplier()); assertEquals(supplierOrder.getSupplier(), suppOrdFound.getSupplier());
        assertEquals(supplierOrder.getSupplierOrderLines(), suppOrdFound.getSupplierOrderLines());
    }

    /**
     * This test is used to ensure that the supplier order repository can't find a supplier order if we give a wrong id.
     */
    @Test
    public void notFoundById() {
        // Execution
        Optional<SupplierOrder> supplierOrderOptional = suppOrderRepository.findById(999L);
        // Verification
        log.info("SUPPLIER ORDERS TEST - NOT FOUND BY ID - Supplier order verifications");
        assertFalse(supplierOrderOptional.isPresent());
    }

    @Test
    public void findBySupplierId() {
        // Initialization
        EntityManager em = testEntityManager.getEntityManager();

        Supplier teklist = new Supplier("Teklist", "hwitnall1@csmonitor.com", null, null);
        em.persist(teklist);
        log.info("SUPPLIER ORDERS TEST - FIND BY SUPPLIER ID - New supplier saved: " + teklist);

        Supplier mymm = new Supplier("Mymm", "iheintzsch2@oakley.com", null, null);
        em.persist(mymm);
        log.info("SUPPLIER ORDERS TEST - FIND BY SUPPLIER ID - New supplier saved: " + mymm);

        SupplierOrder todayOrder = new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3),
                false, false, teklist);
        em.persist(todayOrder);
        log.info("SUPPLIER ORDERS TEST - FIND BY SUPPLIER ID - New supplier order saved: " + todayOrder);

        SupplierOrder twoDaysOrder = new SupplierOrder(LocalDate.now().plusDays(2), LocalDate.now().plusDays(10),
                false, false, mymm);
        em.persist(twoDaysOrder);
        log.info("SUPPLIER ORDERS TEST - FIND BY SUPPLIER ID - New supplier order saved: " + twoDaysOrder);

        SupplierOrder nextMonthOrder = new SupplierOrder(LocalDate.now().plusMonths(1),
                LocalDate.now().plusMonths(1).plusDays(3), false, false, teklist);
        em.persist(nextMonthOrder);
        log.info("SUPPLIER ORDERS TEST - FIND BY SUPPLIER ID - New supplier order saved: " + nextMonthOrder);

        // Execution
        Specification<SupplierOrder> spec = null;
        Page<SupplierOrder> supplierOrders = suppOrderRepository.findBySupplierId(teklist.getId(), PageRequest.of(0, 10));
        // Verification
        log.info("SUPPLIER ORDERS TEST - FIND BY SUPPLIER ID - Supplier order verifications");
        assertNotNull(supplierOrders);
        assertEquals(2, supplierOrders.getTotalElements());
        assertTrue(supplierOrders.getContent().contains(todayOrder));
        assertTrue(supplierOrders.getContent().contains(nextMonthOrder));
        assertFalse(supplierOrders.getContent().contains(twoDaysOrder));
    }

    @Test
    public void findByDeliveryDate() {
        // Initialization
        EntityManager em = testEntityManager.getEntityManager();

        Supplier teklist = new Supplier("Teklist", "hwitnall1@csmonitor.com", null, null);
        em.persist(teklist);
        log.info("SUPPLIER ORDERS TEST - FIND BY SUPPLIER ID - New supplier saved: " + teklist);

        Supplier mymm = new Supplier("Mymm", "iheintzsch2@oakley.com", null, null);
        em.persist(mymm);
        log.info("SUPPLIER ORDERS TEST - FIND BY SUPPLIER ID - New supplier saved: " + mymm);

        SupplierOrder todayOrder = new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3),
                false, false, teklist);
        em.persist(todayOrder);
        log.info("SUPPLIER ORDERS TEST - FIND BY SUPPLIER ID - New supplier order saved: " + todayOrder);

        SupplierOrder twoDaysOrder = new SupplierOrder(LocalDate.now().plusDays(2), LocalDate.now().plusDays(10),
                false, false, teklist);
        em.persist(twoDaysOrder);
        log.info("SUPPLIER ORDERS TEST - FIND BY SUPPLIER ID - New supplier order saved: " + twoDaysOrder);

        SupplierOrder nextMonthOrder = new SupplierOrder(LocalDate.now().plusMonths(1),
                LocalDate.now().plusMonths(1).plusDays(3), false, false, teklist);
        em.persist(nextMonthOrder);
        log.info("SUPPLIER ORDERS TEST - FIND BY SUPPLIER ID - New supplier order saved: " + nextMonthOrder);

        // Execution
        Page<SupplierOrder> supplierOrders = suppOrderRepository.findByDeliveryDate(twoDaysOrder.getDeliveryDate(), PageRequest.of(0, 10));
        // Verification
        log.info("SUPPLIER ORDERS TEST - FIND BY SUPPLIER ID - Supplier order verifications");
        assertNotNull(supplierOrders);
        assertEquals(1, supplierOrders.getTotalElements());
        assertFalse(supplierOrders.getContent().contains(todayOrder));
        assertFalse(supplierOrders.getContent().contains(nextMonthOrder));
        assertTrue(supplierOrders.getContent().contains(twoDaysOrder));

    }

    @Test
    public void findAll() {
        // Initialization
        EntityManager em = testEntityManager.getEntityManager();

        Supplier teklist = new Supplier("Teklist", "hwitnall1@csmonitor.com", null, null);
        em.persist(teklist);
        log.info("SUPPLIER ORDERS TEST - FIND ALL - New supplier saved: " + teklist);

        Supplier mymm = new Supplier("Mymm", "iheintzsch2@oakley.com", null, null);
        em.persist(mymm);
        log.info("SUPPLIER ORDERS TEST - FIND ALL - New supplier saved: " + mymm);

        SupplierOrder todayOrder = new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3),
                false, false, teklist);
        em.persist(todayOrder);
        log.info("SUPPLIER ORDERS TEST - FIND ALL - New supplier order saved: " + todayOrder);

        SupplierOrder twoDaysOrder = new SupplierOrder(LocalDate.now().plusDays(2), LocalDate.now().plusDays(10),
                false, false, mymm);
        em.persist(twoDaysOrder);
        log.info("SUPPLIER ORDERS TEST - FIND ALL - New supplier order saved: " + twoDaysOrder);

        SupplierOrder nextMonthOrder = new SupplierOrder(LocalDate.now().plusMonths(1),
                LocalDate.now().plusMonths(1).plusDays(3), false, false, teklist);
        em.persist(nextMonthOrder);
        log.info("SUPPLIER ORDERS TEST - FIND ALL - New supplier order saved: " + nextMonthOrder);

        // Execution
        Specification<SupplierOrder> spec = null;
        Page<SupplierOrder> supplierOrders = suppOrderRepository.findAll(spec, PageRequest.of(0, 10));

        // Verification
        log.info("SUPPLIER ORDERS TEST - FIND ALL - Supplier order verifications");
        assertNotNull(supplierOrders);
        assertEquals(3, supplierOrders.getTotalElements());

        assertTrue(supplierOrders.getContent().contains(todayOrder));
        assertTrue(supplierOrders.getContent().contains(twoDaysOrder));
        assertTrue(supplierOrders.getContent().contains(nextMonthOrder));
    }
}