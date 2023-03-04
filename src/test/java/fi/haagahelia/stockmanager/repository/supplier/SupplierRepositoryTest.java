package fi.haagahelia.stockmanager.repository.supplier;

import fi.haagahelia.stockmanager.model.common.Geolocation;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Log4j2
public class SupplierRepositoryTest {

    @Autowired
    private SupplierRepository sRepository;

    @Autowired
    private TestEntityManager testEntityManager;


    @BeforeEach
    public void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        Query deleteSupplier = em.createQuery("DELETE Supplier s");
        deleteSupplier.executeUpdate();
        Query deleteGeolocation = em.createQuery("DELETE Geolocation g");
        deleteGeolocation.executeUpdate();
    }

    /**
     * This test is used to ensure that the supplier repository can find the supplier that corresponds to an id.
     */
    @Test
    public void findById() {
        // Initialization
        Geolocation geolocation = new Geolocation("Fredikanterassi", "1", "00520",
                "Helsinki", "Finland");
        Supplier supplier = new Supplier("Chatterpoint", "jdimitriou0@ning.com", "685-160-7579", geolocation);
        log.debug("SUPPLIER TEST - FIND BY ID - New supplier created: " + supplier);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(geolocation);
        em.persist(supplier);
        em.getTransaction().commit();
        log.debug("SUPPLIER TEST - FIND BY ID - New supplier saved: " + supplier);
        // Execution
        Optional<Supplier> supplierOptional = sRepository.findById(supplier.getId());
        // Verification
        log.debug("SUPPLIER TEST - FIND BY ID - Supplier verifications");
        assertTrue(supplierOptional.isPresent());
        Supplier supplierFound = supplierOptional.get();
        assertNotNull(supplierFound);
        assertNotNull(supplierFound.getId()); assertEquals(supplier.getId(), supplierFound.getId());
        assertNotNull(supplierFound.getName()); assertEquals(supplier.getName(), supplierFound.getName());
        assertNotNull(supplierFound.getEmail()); assertEquals(supplier.getEmail(), supplierFound.getEmail());
        assertNotNull(supplierFound.getPhoneNumber()); assertEquals(supplier.getPhoneNumber(), supplierFound.getPhoneNumber());
        assertNotNull(supplierFound.getGeolocation()); assertEquals(supplier.getGeolocation(), supplierFound.getGeolocation());
    }

    /**
     * This test is used to ensure that the supplier repository will not find a supplier if we give a wrong id.
     */
    @Test
    public void notFoundByID() {
        // Execution
        Optional<Supplier> supplierOptional = sRepository.findById(9999L);
        // Verification
        log.debug("SUPPLIER TEST - NOT FOUND BY ID - Supplier verifications");
        assertFalse(supplierOptional.isPresent());
    }

    /**
     * This test is used to ensure that the supplier repository can find the supplier that corresponds to a name.
     */
    @Test
    public void findByName() {
        // Initialization
        Geolocation geolocation = new Geolocation("Töölönlahdenkatu", "4", "00100",
                "Helsinki", "Finland");
        Supplier supplier = new Supplier("Brainverse", "bdalbey1@youku.com", "344-482-9627", geolocation);
        log.debug("SUPPLIER TEST - NOT FOUND BY NAME - New supplier created: " + supplier);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(geolocation);
        em.persist(supplier);
        em.getTransaction().commit();
        log.debug("SUPPLIER TEST - NOT FOUND BY NAME - New supplier saved: " + supplier);
        // Execution
        Optional<Supplier> supplierOptional = sRepository.findByName(supplier.getName());
        // Verification
        log.debug("SUPPLIER TEST - NOT FOUND BY NAME - Supplier verifications");
        assertTrue(supplierOptional.isPresent());
        Supplier supplierFound = supplierOptional.get();
        assertNotNull(supplierFound);
        assertNotNull(supplierFound.getId()); assertEquals(supplier.getId(), supplierFound.getId());
        assertNotNull(supplierFound.getName()); assertEquals(supplier.getName(), supplierFound.getName());
        assertNotNull(supplierFound.getEmail()); assertEquals(supplier.getEmail(), supplierFound.getEmail());
        assertNotNull(supplierFound.getPhoneNumber()); assertEquals(supplier.getPhoneNumber(), supplierFound.getPhoneNumber());
        assertNotNull(supplierFound.getGeolocation()); assertEquals(supplier.getGeolocation(), supplierFound.getGeolocation());
    }

    /**
     * This test is used to ensure that the supplier repository will not find a supplier if we give a wrong name.
     */
    @Test
    public void notFoundByName() {
        // Execution
        Optional<Supplier> supplierOptional = sRepository.findByName("WRONG NAME");
        // Verification
        log.debug("SUPPLIER TEST - NOT FOUND BY NAME - Supplier verifications");
        assertFalse(supplierOptional.isPresent());
    }

    /**
     * This method is used to ensure that the supplier repository can return if a supplier exists by its name.
     */
    @Test
    public void existsByName() {
        // Initialization
        Supplier supplier = new Supplier("Mybuzz", "apeach2@forbes.com", "333-180-0957", null);
        log.debug("SUPPLIER TEST - EXISTS BY NAME - New supplier created: " + supplier);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(supplier);
        em.getTransaction().commit();
        log.debug("SUPPLIER TEST - EXISTS BY NAME - New supplier saved: " + supplier);
        // Execution
        Boolean result = sRepository.existsByName(supplier.getName());
        // Verification
        log.debug("SUPPLIER TEST - EXISTS BY NAME - Supplier verifications");
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * This method is used to ensure that the supplier repository can return if a supplier exists by its name.
     */
    @Test
    public void doesNotExistByName() {
        // Execution
        Boolean result = sRepository.existsByName("INCORRECT");
        // Verification
        log.debug("SUPPLIER TEST - DOEST NOT EXIST BY NAME - Supplier verifications");
        assertNotNull(result);
        assertFalse(result);
    }

    /**
     * This method is used to ensure that the supplier repository can return if a supplier is located in a geolocation.
     */
    @Test
    public void existsByGeolocationId() {
        // Initialization
        Geolocation geolocation = new Geolocation("Urho Kekkosen katu", "1", "00100",
                "Helsinki", "Finland");
        Supplier supplier = new Supplier("Twitterlist", "cyurkin7@economist.com", "260-680-7154", geolocation);
        log.debug("SUPPLIER TEST - EXISTS BY GEOLOCATION ID - New supplier and geolocation created: "
                + supplier + ";" + geolocation);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(geolocation);
        em.persist(supplier);
        em.getTransaction().commit();
        log.debug("SUPPLIER TEST - EXISTS BY GEOLOCATION ID - New supplier and geolocation saved: "
                + supplier + ";" + geolocation);
        // Execution
        Boolean result = sRepository.existsByGeolocationId(geolocation.getId());
        // Verification
        log.debug("SUPPLIER TEST - EXISTS BY GEOLOCATION ID - Result verifications");
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * This method is used to ensure that the supplier repository can return if a supplier is located in a geolocation.
     */
    @Test
    public void doesNotExistByGeolocationId() {
        // Initialization
        Geolocation geolocation = new Geolocation("Aleksanterinkatu", "52", "00100",
                "Helsinki", "Finland");
        log.debug("SUPPLIER TEST - DOES NOT EXIST BY GEOLOCATION ID - New geolocation created: " + geolocation);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(geolocation);
        em.getTransaction().commit();
        log.debug("SUPPLIER TEST - DOES NOT EXIST BY GEOLOCATION ID - New geolocation saved: " + geolocation);
        // Execution
        Boolean result = sRepository.existsByGeolocationId(geolocation.getId());
        // Verification
        log.debug("SUPPLIER TEST - DOES NOT EXIST BY GEOLOCATION ID - Result verifications");
        assertNotNull(result);
        assertFalse(result);
    }

    /**
     * This method is used to ensure that the supplier repository can return if a supplier is located in a geolocation.
     */
    @Test
    public void existsByWrongGeolocationId() {
        // Execution
        Boolean result = sRepository.existsByGeolocationId(999999L);
        // Verification
        log.debug("SUPPLIER TEST - EXISTS BY WRONG GEOLOCATION ID - Result verifications");
        assertNotNull(result);
        assertFalse(result);
    }
}