package fi.haagahelia.stockmanager.repository.product;

import fi.haagahelia.stockmanager.model.product.brand.Brand;
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
public class BrandRepositoryTest {

    @Autowired
    private BrandRepository bRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @BeforeEach
    public void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        Query deleteBrands = em.createQuery("DELETE Brand  b");
        deleteBrands.executeUpdate();
    }

    /**
     * This test is used to ensure that the brand repository can find the brand that corresponds to an id.
     */
    @Test
    public void findById() {
        // Initialization
        Brand victorinox = new Brand("Victorinox");
        log.debug("BRAND TEST - FIND BY ID - New brand created: " + victorinox);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(victorinox);
        log.debug("BRAND TEST - FIND BY ID - New brand saved: " + victorinox);
        // Execution
        Optional<Brand> brandOptional = bRepository.findById(victorinox.getId());
        // Verification
        log.debug("BRAND TEST - FIND BY ID - Brand verification");
        assertTrue(brandOptional.isPresent());
        Brand brandFound = brandOptional.get();
        assertNotNull(brandFound.getId()); assertEquals(victorinox.getId(), brandFound.getId());
        assertNotNull(brandFound.getName()); assertEquals(victorinox.getName(), brandFound.getName());
    }

    /**
     * This test is used to ensure that the brand repository will not find a brand if we give a wrong id.
     */
    @Test
    public void notFoundById() {
        // Execution
        Optional<Brand> brandOptional = bRepository.findById(9999999L);
        // Verification
        log.debug("BRAND TEST - NOT FOUND BY ID - Brand verification");
        assertFalse(brandOptional.isPresent());
    }

    /**
     * This method is used to ensure that the brand repository can return if a brand exists.
     */
    @Test
    public void existsByName() {
        // Initialization
        Brand rolex = new Brand("Rolex");
        log.debug("BRAND TEST - EXISTS BY NAME - New brand created: " + rolex);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(rolex);
        log.debug("BRAND TEST - EXISTS BY NAME - New brand saved: " + rolex);
        // Execution
        Boolean result = bRepository.existsByName(rolex.getName());
        // Verification
        log.debug("BRAND TEST - EXISTS BY NAME - Brand exists verification");
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * This method is used to ensure that the brand repository can return if a brand exists.
     */
    @Test
    public void doesNotExistByName() {
        // Execution
        Boolean result = bRepository.existsByName("NAME DOES NOT EXIST");
        // Verification
        log.debug("BRAND TEST - EXISTS BY NAME - Brand does not exist verification");
        assertNotNull(result);
        assertFalse(result);
    }
}