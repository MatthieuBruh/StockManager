package fi.haagahelia.stockmanager.repository.supplier.order;

import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLine;
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
public class SupplierOrderLineRepositoryTest {

    @Autowired
    private SupplierOrderLineRepository linesRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private Product product;
    private SupplierOrder supplierOrder;
    private SupplierOrderLine supplierOrderLine;

    @BeforeEach
    public void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        em.createQuery("DELETE SupplierOrderLine").executeUpdate();
        em.createQuery("DELETE SupplierOrder ").executeUpdate();
        em.createQuery("DELETE Product").executeUpdate();
        em.createQuery("DELETE Supplier").executeUpdate();
        em.createQuery("DELETE Brand").executeUpdate();
        em.createQuery("DELETE Category").executeUpdate();

        Category category = new Category("Motherboard", "This category is for the motherboards");
        em.persist(category);
        log.debug("SUPPLIER ORDER LINE TEST - SET UP - New category saved: " + category);

        Brand brand = new Brand("Asus");
        em.persist(brand);
        log.debug("SUPPLIER ORDER LINE TEST - SET UP - New brand saved: " + brand);

        Supplier supplier = new Supplier("Camido", "cioannou0@chron.com", null, null);
        em.persist(supplier);
        log.info("SUPPLIER ORDER LINE TEST - SET UP - New supplier saved: " + supplier);

        product = new Product("ROG STRIX Z790-H GAMING WIFI", "Motherboard for gaming",
                200.0, 300.0, 20, 2, 5, brand, category, supplier);
        em.persist(product);
        log.info("SUPPLIER ORDER LINE TEST - SET UP - New supplier saved: " + supplier);

        supplierOrder = new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier);
        em.persist(supplierOrder);
        log.info("SUPPLIER ORDER LINE TEST - SET UP - New order saved: " + supplierOrder);

        supplierOrderLine = new SupplierOrderLine(10, 5.0, supplierOrder, product);
        em.persist(supplierOrderLine);
        log.info("SUPPLIER ORDER LINE TEST - SET UP - New order saved: " + supplierOrderLine);
    }

    @Test
    public void findBySupplierOrderId() {
        // Execution
        Page<SupplierOrderLine> lines = linesRepository.findBySupplierOrderId(supplierOrder.getId(), PageRequest.of(0, 10));
        // Page<SupplierOrderLine> lines = Page.empty();
        // Verification
        assertNotNull(lines);
        assertEquals(1, lines.getTotalElements());
        SupplierOrderLine foundLine = lines.getContent().get(0);
        assertEquals(supplierOrderLine, foundLine);
        assertNotNull(foundLine.getSupplierOrderLinePK());
        assertEquals(supplierOrderLine.getSupplierOrderLinePK(), foundLine.getSupplierOrderLinePK());
        assertNotNull(foundLine.getSupplierOrder());
        assertEquals(supplierOrderLine.getSupplierOrder(), foundLine.getSupplierOrder());
        assertNotNull(foundLine.getProduct());
        assertEquals(supplierOrderLine.getProduct(), foundLine.getProduct());
        assertNotNull(foundLine.getQuantity());
        assertEquals(supplierOrderLine.getQuantity(), foundLine.getQuantity());
        assertNotNull(foundLine.getBuyPrice());
        assertEquals(supplierOrderLine.getBuyPrice(), foundLine.getBuyPrice());
    }

    @Test
    public void findBySupplierOrderIdAndProductId() {
        // Execution
        Optional<SupplierOrderLine> lineOptional = linesRepository.findBySupplierOrderIdAndProductId(supplierOrder.getId(), product.getId());
        // Verification
        assertNotNull(lineOptional);
        assertTrue(lineOptional.isPresent());
        SupplierOrderLine foundLine = lineOptional.get();
        assertEquals(supplierOrderLine, foundLine);
        assertNotNull(foundLine.getSupplierOrderLinePK()); assertEquals(supplierOrderLine.getSupplierOrderLinePK(), foundLine.getSupplierOrderLinePK());
        assertNotNull(foundLine.getSupplierOrder()); assertEquals(supplierOrderLine.getSupplierOrder(), foundLine.getSupplierOrder());
        assertNotNull(foundLine.getProduct()); assertEquals(supplierOrderLine.getProduct(), foundLine.getProduct());
        assertNotNull(foundLine.getQuantity()); assertEquals(supplierOrderLine.getQuantity(), foundLine.getQuantity());
        assertNotNull(foundLine.getBuyPrice()); assertEquals(supplierOrderLine.getBuyPrice(), foundLine.getBuyPrice());
    }

    @Test
    public void deleteBySupplierOrderIdAndProductId() {
        // Execution
        linesRepository.deleteBySupplierOrderIdAndProductId(supplierOrder.getId(), product.getId());

        // Verification
        Optional<SupplierOrderLine> lineOptional = linesRepository.findBySupplierOrderIdAndProductId(supplierOrder.getId(), product.getId());
        assertFalse(lineOptional.isPresent());
    }

    @Test
    public void existsByProductId() {
        // Execution
        boolean exists = linesRepository.existsByProductId(product.getId());
        // Verification
        assertTrue(exists);
    }

    @Test
    public void doesNotExistByProductId() {
        // Execution
        boolean exists = linesRepository.existsByProductId(999L);
        // Verification
        assertFalse(exists);
    }

    @Test
    public void existsBySupplierOrderIdAndProductId() {
        // Execution
        boolean exists = linesRepository.existsBySupplierOrderIdAndProductId(supplierOrder.getId(), product.getId());
        // Verification
        assertTrue(exists);
    }

    @Test
    public void doesNotExistBySupplierOrderIdAndProductId() {
        // Execution
        boolean exists = linesRepository.existsBySupplierOrderIdAndProductId(888L, 9999L);
        // Verification
        assertFalse(exists);
    }
}