package fi.haagahelia.stockmanager.repository.customer;

import fi.haagahelia.stockmanager.model.common.Geolocation;
import fi.haagahelia.stockmanager.model.customer.Customer;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Log4j2
public class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository cRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @BeforeEach
    public void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        Query deleteCustomer = em.createQuery("DELETE Customer  c");
        deleteCustomer.executeUpdate();
        Query deleteGeolocation = em.createQuery("DELETE Geolocation g");
        deleteGeolocation.executeUpdate();
    }

    /**
     * This test is used to ensure that the customer repository can find the customer that corresponds to an id.
     */
    @Test
    public void findById() {
        // Initialization
        Customer customer = new Customer("Renie", "Austing", "rausting0@pbs.org");
        log.debug("CUSTOMER TEST - FIND BY ID - New customer created: " + customer);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(customer);
        log.debug("CUSTOMER TEST - FIND BY ID - New customer saved: " + customer);
        // Execution
        Optional<Customer> customerOptional = cRepository.findById(customer.getId());
        // Verification
        log.debug("CUSTOMER TEST - FIND BY ID - Customer verifications");
        assertTrue(customerOptional.isPresent());
        Customer customerFound = customerOptional.get();
        assertNotNull(customerFound.getId()); assertEquals(customer.getId(), customerFound.getId());
        assertNotNull(customerFound.getFirstName()); assertEquals(customer.getFirstName(), customerFound.getFirstName());
        assertNotNull(customerFound.getLastName()); assertEquals(customer.getLastName(), customerFound.getLastName());
        assertNotNull(customerFound.getEmail()); assertEquals(customer.getEmail(), customerFound.getEmail());
        assertNull(customerFound.getGeolocation());
    }

    /**
     * This test is used to ensure that the customer repository will not find a customer if we give a wrong id.
     */
    @Test
    public void notFoundById() {
        // Execution
        Optional<Customer> customerOptional = cRepository.findById(999L);
        // Verification
        log.debug("CUSTOMER TEST - NOT FOUND BY ID - Customer verifications");
        assertFalse(customerOptional.isPresent());
    }

    /**
     * This test is used to ensure that the customer repository can find the customer that corresponds to an email.
     */
    @Test
    public void findByEmail() {
        // Initialization
        Geolocation geolocation = new Geolocation("Ratapihantie", "13", "00520",
                "Helsinki", "Finland");
        Customer customer = new Customer("Archy", "Riggeard", "ariggeard1@princeton.edu", geolocation);
        log.debug("CUSTOMER TEST - FIND BY EMAIL - New customer and geolocation created: " + customer + ";" + geolocation);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(geolocation);
        em.persist(customer);
        em.getTransaction().commit();
        log.debug("CUSTOMER TEST - FIND BY EMAIL - New customer and geolocation saved: " + customer + ";" + geolocation);
        // Execution
        Optional<Customer> customerOptional = cRepository.findByEmail(customer.getEmail());
        // Verification
        log.debug("CUSTOMER TEST - FIND BY EMAIL - Customer verifications");
        assertTrue(customerOptional.isPresent());
        Customer customerFound = customerOptional.get();
        assertNotNull(customerFound.getId()); assertEquals(customer.getId(), customerFound.getId());
        assertNotNull(customerFound.getFirstName()); assertEquals(customer.getFirstName(), customerFound.getFirstName());
        assertNotNull(customerFound.getLastName()); assertEquals(customer.getLastName(), customerFound.getLastName());
        assertNotNull(customerFound.getEmail()); assertEquals(customer.getEmail(), customerFound.getEmail());
        assertNotNull(customerFound.getGeolocation()); assertEquals(customer.getGeolocation(), customerFound.getGeolocation());
    }

    /**
     * This test is used to ensure that the customer repository will not find a customer if we give a wrong email.
     */
    @Test
    public void notFoundByEmail() {
        // Execution
        Optional<Customer> customerOptional = cRepository.findByEmail("WRONG EMAIL");
        // Verification
        log.debug("CUSTOMER TEST - NOT FOUND BY EMAIL - Customer verifications");
        assertFalse(customerOptional.isPresent());
    }

    /**
     * This method is used to ensure that the customer repository can return if a customer exists by its email.
     */
    @Test
    public void existsByEmail() {
        // Initialization
        Customer customer = new Customer("Sonja", "Gaskell", "sgaskell2@barnesandnoble.com");
        log.debug("CUSTOMER TEST - EXISTS BY EMAIL - New customer created: " + customer);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(customer);
        log.debug("CUSTOMER TEST - EXISTS BY EMAIL - New customer saved: " + customer);
        // Execution
        Boolean result = cRepository.existsByEmail(customer.getEmail());
        // Verification
        log.debug("CUSTOMER TEST - EXISTS BY EMAIL - Customer verifications");
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * This method is used to ensure that the customer repository can return if a customer exists by its email.
     */
    @Test
    public void doesNotExistByEmail() {
        // Execution
        Boolean result = cRepository.existsByEmail("INCORRECT");
        // Verification
        log.debug("CUSTOMER TEST - DOES NOT EXIST BY EMAIL - Customer verifications");
        assertNotNull(result);
        assertFalse(result);
    }

    /**
     * This method is used to ensure that the customer repository can return if a supplier is located in a geolocation.
     */
    @Test
    public void existsByGeolocationId() {
        // Initialization
        Geolocation geolocation = new Geolocation("Haartmaninkatu 4 Rakennus", "12", "00290",
                "Helsinki", "Finland");
        Customer customer = new Customer("Emmott", "McInulty", "emcinulty3@joomla.org", geolocation);
        log.debug("CUSTOMER TEST - EXISTS BY GEOLOCATION ID - New customer and geolocation created: "
                + customer + ";" + geolocation);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(geolocation);
        em.persist(customer);
        em.getTransaction().commit();
        log.debug("CUSTOMER TEST - EXISTS BY GEOLOCATION ID - New customer and geolocation saved: "
                + customer + ";" + geolocation);
        // Execution
        Boolean result = cRepository.existsByGeolocationId(geolocation.getId());
        // Verification
        log.debug("CUSTOMER TEST - EXISTS BY GEOLOCATION ID - Customer verifications");
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * This method is used to ensure that the customer repository can return if a supplier is located in a geolocation.
     */
    @Test
    public void doesNotExistByGeolocationId() {
        // Initialization
        Geolocation geolocation = new Geolocation("Pajuniityntie", "11", "00320",
                "Helsinki", "Finland");
        log.debug("CUSTOMER TEST - DOES NOT EXIST BY GEOLOCATION ID - New geolocation created: " + geolocation);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(geolocation);
        em.getTransaction().commit();
        log.debug("CUSTOMER TEST - DOES NOT EXIST BY GEOLOCATION ID - New geolocation saved: " + geolocation);
        // Execution
        Boolean result = cRepository.existsByGeolocationId(geolocation.getId());
        // Verification
        log.debug("CUSTOMER TEST - DOES NOT EXIST BY GEOLOCATION ID - Result verification");
        assertNotNull(result);
        assertFalse(result);
    }

    /**
     * This method is used to ensure that the customer repository can return if a customer is located in a geolocation.
     */
    @Test
    public void existsByWrongGeolocationId() {
        // Execution
        Boolean result = cRepository.existsByGeolocationId(9999L);
        // Verification
        log.debug("CUSTOMER TEST - EXISTS BY WRONG GEOLOCATION ID - Result verification");
        assertNotNull(result);
        assertFalse(result);
    }
}