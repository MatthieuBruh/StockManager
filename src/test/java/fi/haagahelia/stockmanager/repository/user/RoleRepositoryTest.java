package fi.haagahelia.stockmanager.repository.user;

import fi.haagahelia.stockmanager.model.user.Role;
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
public class RoleRepositoryTest {

    @Autowired
    private RoleRepository rRepository;

    @Autowired
    private TestEntityManager testEntityManager;


    @BeforeEach
    void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        Query query = em.createQuery("DELETE Role  r");
        query.executeUpdate();
    }


    /**
     * This test is used to ensure that the method findById of the role repository is able to find a persisted role.
     * @throws Exception An exception that is throw if the test does not pass.
     */
    @Test
    public void findById() throws Exception {
        // Initialization
        Role role = new Role( "ROLE_FindById", "This is the role that should be find by id.");
        log.debug("ROLE TEST - FIND BY ID - New role created: " + role);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(role);
        em.getTransaction().commit();
        log.debug("ROLE TEST - FIND BY ID - New role saved: " + role);
        // Execution
        Optional<Role> savedRoleOptional = rRepository.findById(role.getId());
        // Verification
        log.debug("ROLE TEST - FIND BY ID - Role verification");
        assertTrue(savedRoleOptional.isPresent());
        Role savedRole = savedRoleOptional.get();
        assertNotNull(savedRole.getId());
        assertNotNull(savedRole.getName());
        assertNotNull(savedRole.getDescription());
        assertFalse(savedRole.getName().isEmpty());
        assertFalse(savedRole.getDescription().isEmpty());
    }

    /**
     * This test is used to ensure that the role repository can't find a non-existing role by a wrong id.
     */
    @Test
    public void notFoundById() {
        // Execution
        Optional<Role> roleOptional = rRepository.findById(99999999L);
        // Verification
        log.debug("ROLE TEST - NOT FOUND BY ID - Role verification");
        assertFalse(roleOptional.isPresent());
    }

    /**
     * This test is used to ensure that we can find a role by its name.
     */
    @Test
    public void findByName() {
        // Initialization
        Role role = new Role( "ROLE_FindByName", "This is the role that should be find by its name.");
        log.debug("ROLE TEST - FIND BY NAME - New role created: " + role);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(role);
        em.getTransaction().commit();
        log.debug("ROLE TEST - FIND BY NAME - New role saved: " + role);
        // Execution
        Optional<Role> savedRoleOptional = rRepository.findByName(role.getName());
        // Verification
        log.debug("ROLE TEST - FIND BY NAME - Role verification");
        assertTrue(savedRoleOptional.isPresent());
        Role savedRole = savedRoleOptional.get();
        assertNotNull(savedRole.getId());
        assertNotNull(savedRole.getName());
        assertNotNull(savedRole.getDescription());
        assertFalse(savedRole.getName().isEmpty());
        assertFalse(savedRole.getDescription().isEmpty());
        assertEquals(role.getName(), savedRole.getName());
        assertEquals(role.getDescription(), savedRole.getDescription());
    }

    /**
     * This test is used to ensure that the role repository can't find a name that does not exist.
     */
    @Test
    public void notFoundByName() {
        // Execution
        Optional<Role> roleOptional = rRepository.findByName("NO NAME");
        // Verification
        log.debug("ROLE TEST - NOT FOUND BY NAME - Role verification");
        assertFalse(roleOptional.isPresent());
    }

    /**
     * This test is used t ensure that the role repository can say if a role exists or not by a given name.
     */
    @Test
    public void existsByName() {
        // Initialization
        Role role = new Role( "ROLE_ExistsByName", "This is the role that should exists by its name.");
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(role);
        em.getTransaction().commit();
        log.debug("ROLE TEST - EXISTS BY NAME - New role saved: " + role);
        // Execution
        Boolean result = rRepository.existsByName(role.getName());
        // Verification
        log.debug("ROLE TEST - EXISTS BY NAME - Role verification");
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * This test is used to ensure that a non-existing role cannot be considered as existing by the role repository.
     */
    @Test
    public void doestExistByName() {
        // Execution
        Boolean result = rRepository.existsByName("NO NAME EXISTING");
        // Verification
        log.debug("ROLE TEST - DOES NOT BY NAME - Role verification");
        assertNotNull(result);
        assertFalse(result);
    }
}