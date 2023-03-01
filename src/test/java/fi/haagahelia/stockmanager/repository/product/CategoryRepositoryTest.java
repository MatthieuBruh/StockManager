package fi.haagahelia.stockmanager.repository.product;

import fi.haagahelia.stockmanager.model.product.category.Category;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository cRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @BeforeEach
    public void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        Query deleteCategories = em.createQuery("DELETE Category c");
        deleteCategories.executeUpdate();
    }

    /**
     * This test is used to ensure that the category repository can find the category that corresponds to an id.
     */
    @Test
    public void findById() {
        // Initialization
        Category pie = new Category("Pies", "This is for products that are considered as a pie.");
        log.debug("CATEGORY TEST - FIND BY ID - New category created: " + pie);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(pie);
        em.getTransaction().commit();
        log.debug("CATEGORY TEST - FIND BY ID - New category saved: " + pie);
        // Execution
        Optional<Category> categoryOptional = cRepository.findById(pie.getId());
        // Verification
        log.debug("CATEGORY TEST - FIND BY ID - Category verifications");
        assertTrue(categoryOptional.isPresent());
        Category categoryFound = categoryOptional.get();
        assertNotNull(categoryFound);
        assertNotNull(categoryFound.getId()); assertEquals(pie.getId(), categoryFound.getId());
        assertNotNull(categoryFound.getName()); assertEquals(pie.getName(), categoryFound.getName());
        assertNotNull(categoryFound.getDescription()); assertEquals(pie.getDescription(), categoryFound.getDescription());
        assertEquals(pie, categoryFound);
    }

    /**
     * This test is used to ensure that the category repository will not find a category if we give a wrong id.
     */
    @Test
    public void notFoundById() {
        // Execution
        Optional<Category> categoryOptional = cRepository.findById(99999L);
        // Verification
        log.debug("CATEGORY TEST - NOT FOUND BY ID - Category verifications");
        assertFalse(categoryOptional.isPresent());
    }

    /**
     * This method is used to ensure that the category repository can return if a category exists.
     */
    @Test
    public void existsByName() {
        // Initialization
        Category cake = new Category("Cake", "This is for the products that are considered as a cake.");
        log.debug("CATEGORY TEST - EXISTS BY NAME - New category created: " + cake);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(cake);
        em.getTransaction().commit();
        log.debug("CATEGORY TEST - EXISTS BY NAME - New category saved: " + cake);
        // Execution
        Boolean result = cRepository.existsByName(cake.getName());
        // Verification
        log.debug("CATEGORY TEST - EXISTS BY NAME - Category verifications");
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * This method is used to ensure that the category repository can return if a category exists.
     */
    @Test
    public void doesNotExistsByName() {
        // Execution
        Boolean result = cRepository.existsByName("WRONG NAME");
        // Verification
        log.debug("CATEGORY TEST - DOES NOT EXIST BY NAME - Category verifications");
        assertNotNull(result);
        assertFalse(result);
    }
}