package fi.haagahelia.stockmanager.repository.product;

import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Log4j2
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository pRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @BeforeEach
    public void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        Query deleteProduct = em.createQuery("DELETE Product p"); deleteProduct.executeUpdate();
        Query deleteCategory = em.createQuery("DELETE Category c"); deleteCategory.executeUpdate();
        Query deleteBrand = em.createQuery("DELETE Brand b"); deleteBrand.executeUpdate();
        Query deleteSupplier = em.createQuery("DELETE Supplier s"); deleteSupplier.executeUpdate();
    }

    /**
     * This test is used to ensure that the product repository can find a product by its id.
     */
    @Test
    public void findById() {
        // Initialization
        EntityManager em = testEntityManager.getEntityManager();

        Category category = new Category("Motherboard", "This category is for the motherboards");
        em.persist(category);
        log.debug("PRODUCT TEST - FIND BY ID - New category saved: " + category);

        Brand brand = new Brand("Asus");
        em.persist(brand);
        log.debug("PRODUCT TEST - FIND BY ID - New brand saved: " + brand);

        Supplier supplier = new Supplier("Chatterbridge", "astorck1@usgs.gov", null, null);
        em.persist(supplier);
        log.debug("PRODUCT TEST - FIND BY ID - New supplier saved: " + supplier);

        Product product = new Product("ROG STRIX Z790-H GAMING WIFI", "Motherboard for gaming",
                200.0, 300.0, 20, 2, 5, brand, category, supplier);
        em.persist(product);
        log.debug("PRODUCT TEST - FIND BY ID - New product saved: " + product);

        em.getTransaction().commit();
        // Execution
        Optional<Product> productOptional = pRepository.findById(product.getId());
        // Verification
        log.debug("PRODUCT TEST - FIND BY ID - Product verifications");
        assertTrue(productOptional.isPresent());
        Product prodFound = productOptional.get();
        assertNotNull(prodFound); assertEquals(product, prodFound);
        assertNotNull(prodFound.getId()); assertEquals(product.getId(), prodFound.getId());
        assertNotNull(prodFound.getName()); assertEquals(product.getName(), prodFound.getName());
        assertNotNull(prodFound.getDescription()); assertEquals(product.getDescription(), prodFound.getDescription());
        assertNotNull(prodFound.getPurchasePrice()); assertEquals(product.getPurchasePrice(), prodFound.getPurchasePrice());
        assertNotNull(prodFound.getSalePrice()); assertEquals(product.getSalePrice(), prodFound.getSalePrice());
        assertNotNull(prodFound.getStock()); assertEquals(product.getStock(), prodFound.getStock());
        assertNotNull(prodFound.getMinStock()); assertEquals(product.getMinStock(), prodFound.getMinStock());
        assertNotNull(prodFound.getBatchSize()); assertEquals(product.getBatchSize(), prodFound.getBatchSize());
        assertNotNull(prodFound.getBrand()); assertEquals(product.getBrand(), prodFound.getBrand());
        assertNotNull(prodFound.getCategory()); assertEquals(product.getCategory(), prodFound.getCategory());
        assertNotNull(prodFound.getSupplier()); assertEquals(product.getSupplier(), prodFound.getSupplier());
    }

    /**
     * This test is used to ensure that the product repository can't find a product if we give a wrong id.
     */
    @Test
    public void notFoundById() {
        // Execution
        Optional<Product> productOptional = pRepository.findById(999L);
        // Verification
        log.debug("PRODUCT TEST - NOT FOUND BY ID - Product isPresent verification");
        assertFalse(productOptional.isPresent());
    }

    /**
     * This test is used to ensure that the product repository can say if a product exists by its name and the supplier id.
     */
    @Test
    public void existsByNameAndSupplierId() {
        // Initialization
        EntityManager em = testEntityManager.getEntityManager();

        Category category = new Category("CPU", "This category is for the CPUs");
        em.persist(category);
        log.debug("PRODUCT TEST - EXISTS BY NAME AND SUPPLIER ID - New category saved: " + category);

        Brand brand = new Brand("AMD");
        em.persist(brand);
        log.debug("PRODUCT TEST - EXISTS BY NAME AND SUPPLIER ID - New brand saved: " + brand);

        Supplier supplier = new Supplier("Mydo", "rriddel3@tuttocitta.it", null, null);
        em.persist(supplier);
        log.debug("PRODUCT TEST - EXISTS BY NAME AND SUPPLIER ID - New supplier saved: " + supplier);

        Product product = new Product("Ryzen 7 3700X", "CPU 8-Core",
                379.0, 400.0, 20, 2, 5, brand, category, supplier);
        em.persist(product);
        log.debug("PRODUCT TEST - EXISTS BY NAME AND SUPPLIER ID - New product saved: " + product);

        em.getTransaction().commit();
        // Execution
        Boolean result = pRepository.existsByNameAndSupplierId(product.getName(), product.getSupplier().getId());
        // Verification
        log.debug("PRODUCT TEST - EXISTS BY NAME AND SUPPLIER ID - Result verifications");
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * This test is used to ensure that the product repository can say if a product exists by its name and the supplier id.
     */
    @Test
    public void doesNotExistByNameAndSupplierId() {
        // Execution
        Boolean result = pRepository.existsByNameAndSupplierId("UNNAMED", 999L);
        // Verification
        log.debug("PRODUCT TEST - DOES NOT EXIST BY NAME AND SUPPLIER ID - Result verification");
        assertNotNull(result);
        assertFalse(result);
    }

    /**
     * This test is used to ensure that the product repository can find all the products that have a low stock.
     */
    @Test
    public void findByStockIsLessThanMinStock() {
        // Initialization
        EntityManager em = testEntityManager.getEntityManager();

        Category category = new Category("GPU", "This category is for the GPUs");
        em.persist(category);
        log.debug("PRODUCT TEST - FIND BY STOCK IS LOW - New category saved: " + category);

        Brand brand = new Brand("Nvidia");
        em.persist(brand);
        log.debug("PRODUCT TEST - FIND BY STOCK IS LOW - New brand saved: " + brand);

        Supplier supplier = new Supplier("Edgetag", "disaaksohn6@multiply.com", null, null);
        em.persist(supplier);
        log.debug("PRODUCT TEST - FIND BY STOCK IS LOW - New supplier saved: " + supplier);

        Product rtx3090 = new Product("GeForce RTX 3090 TI", "GPU for gaming",
                1200.30, 1529.0, 1, 5, 10, brand, category, supplier);
        em.persist(rtx3090);
        log.debug("PRODUCT TEST - FIND BY STOCK IS LOW - New product saved: " + rtx3090);

        Product rtx2070 = new Product("GeForce RTX 2070", "GPU for gaming",
                950.0, 1100.0, 10, 5, 2, brand, category, supplier);
        em.persist(rtx2070);
        log.debug("PRODUCT TEST - FIND BY STOCK IS LOW - New product saved: " + rtx2070);

        Product rtx4090 = new Product("GeForce RTX 4090", "GPU for gaming",
                3500.30, 8000.0, 0, 5, 10, brand, category, supplier);
        em.persist(rtx4090);
        log.debug("PRODUCT TEST - FIND BY STOCK IS LOW - New product saved: " + rtx4090);

        em.getTransaction().commit();
        // Execution
        Page<Product> productsFound = pRepository.findByStockIsLessThanMinStock(null, PageRequest.of(0, 10));
        // Verification
        log.debug("PRODUCT TEST - FIND BY STOCK IS LOW - Products found verifications");
        assertNotNull(productsFound);
        assertTrue(productsFound.getTotalElements() > 0); assertEquals(2, productsFound.getTotalElements());
        assertTrue(productsFound.getContent().contains(rtx4090));
        assertTrue(productsFound.getContent().contains(rtx3090));
        assertFalse(productsFound.getContent().contains(rtx2070));
    }

    /**
     * This test is used to ensure that the product repository can say if a product is related to a brand.
     */
    @Test
    public void existsByBrandId() {
        // Initialization
        EntityManager em = testEntityManager.getEntityManager();

        Category category = new Category("SSD", "This category is for the SSD");
        em.persist(category);
        log.debug("PRODUCT TEST - EXISTS BY BRAND ID - New category saved: " + category);

        Brand samsung = new Brand("Samsung");
        em.persist(samsung);
        log.debug("PRODUCT TEST - EXISTS BY BRAND ID - New brand saved: " + samsung);

        Brand sandisk = new Brand("SanDisk");
        em.persist(sandisk);
        log.debug("PRODUCT TEST - EXISTS BY BRAND ID - New brand saved: " + sandisk);

        Supplier supplier = new Supplier("Yata", "valderman8@sina.com.cn", null, null);
        em.persist(supplier);
        log.debug("PRODUCT TEST - EXISTS BY BRAND ID - New supplier saved: " + supplier);

        Product sanDiskExtreme = new Product("Extreme Portable", "3000 GB.",
                100.30, 150.10, 1, 5, 10, sandisk, category, supplier);
        em.persist(sanDiskExtreme);
        log.debug("PRODUCT TEST - EXISTS BY BRAND ID - New product saved: " + sanDiskExtreme);

        em.getTransaction().commit();
        // Execution
        Boolean sandDiskResult = pRepository.existsByBrandId(sandisk.getId());
        Boolean samsungResult = pRepository.existsByBrandId(samsung.getId());
        // Verification
        log.debug("PRODUCT TEST - FIND BY STOCK IS LOW - Verifications");
        assertNotNull(sandDiskResult); assertTrue(sandDiskResult);
        assertNotNull(samsungResult); assertFalse(samsungResult);
    }

    /**
     * This test is used to ensure that the product repository can say if a product is related to a category.
     */
    @Test
    public void existsByCategoryId() {
        // Initialization
        EntityManager em = testEntityManager.getEntityManager();

        Category ssdCategory = new Category("SSD", "This category is for the SSD");
        em.persist(ssdCategory);
        log.debug("PRODUCT TEST - EXISTS BY CATEGORY ID - New category saved: " + ssdCategory);

        Category hddCategory = new Category("HDD", "This category is for the HDD");
        em.persist(hddCategory);
        log.debug("PRODUCT TEST - EXISTS BY CATEGORY ID - New category saved: " + hddCategory);

        Brand samsung = new Brand("Samsung");
        em.persist(samsung);
        log.debug("PRODUCT TEST - EXISTS BY BRAND ID - New brand saved: " + samsung);

        Supplier supplier = new Supplier("Jazzy", "rdedama@deliciousdays.com", null, null);
        em.persist(supplier);
        log.debug("PRODUCT TEST - EXISTS BY BRAND ID - New supplier saved: " + supplier);

        Product t7TitanGrey = new Product("T7 Titan Grey", "3000 GB.",
                80.50, 99.90, 1, 5, 10, samsung, ssdCategory, supplier);
        em.persist(t7TitanGrey);
        log.debug("PRODUCT TEST - EXISTS BY BRAND ID - New product saved: " + t7TitanGrey);

        em.getTransaction().commit();
        // Execution
        Boolean ssdResult = pRepository.existsByCategoryId(ssdCategory.getId());
        Boolean hddResult = pRepository.existsByCategoryId(hddCategory.getId());
        // Verification
        log.debug("PRODUCT TEST - FIND BY STOCK IS LOW - Verifications");
        assertNotNull(ssdResult); assertTrue(ssdResult);
        assertNotNull(hddResult); assertFalse(hddResult);
    }
}