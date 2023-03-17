package fi.haagahelia.stockmanager.repository.user;

import fi.haagahelia.stockmanager.model.user.Employee;
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
public class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository eRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @BeforeEach
    void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        Query deleteEmployeesQuery = em.createQuery("DELETE Employee e");
        deleteEmployeesQuery.executeUpdate();
        Query deleteRolesQuery = em.createQuery("DELETE Role  r");
        deleteRolesQuery.executeUpdate();
    }

    /**
     * This test is used to verify that the employee repository can find a saved employee by his id.
     * @throws Exception An exception that is throw if the test does not pass.
     */
    @Test
    public void findById() throws Exception {
        // Initialization
        Employee employee = new Employee("findbyid@haaga.fi", "johnD",
                "John", "Doe", "FAKE PASSWORD", false, true);
        log.debug("EMPLOYEE TEST - FIND BY ID - New employee created: " + employee);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(employee);
        em.getTransaction().commit();
        log.debug("EMPLOYEE TEST - FIND BY ID - New employee saved: " + employee);
        // Execution
        Optional<Employee> foundEmployee = eRepository.findById(employee.getId());
        // Verification
        log.debug("EMPLOYEE TEST - FIND BY ID - Employee verification");
        assertTrue(foundEmployee.isPresent());
        Employee savedEmployee = foundEmployee.get();
        assertNotNull(savedEmployee.getId()); assertEquals(employee.getId(), savedEmployee.getId());
        assertNotNull(savedEmployee.getEmail()); assertEquals(employee.getEmail(), savedEmployee.getEmail());
        assertNotNull(savedEmployee.getUsername()); assertEquals(employee.getUsername(), savedEmployee.getUsername());
        assertNotNull(savedEmployee.getFirstName()); assertEquals(employee.getFirstName(), savedEmployee.getFirstName());
        assertNotNull(savedEmployee.getLastName()); assertEquals(employee.getLastName(), savedEmployee.getLastName());
        assertNotNull(savedEmployee.getPassword()); assertEquals(employee.getPassword(), savedEmployee.getPassword());
        assertNotNull(savedEmployee.getActive()); assertEquals(employee.getActive(), savedEmployee.getActive());
        assertNotNull(savedEmployee.getBlocked()); assertEquals(employee.getBlocked(), savedEmployee.getBlocked());
        assertEquals(employee.getRoles().size(), savedEmployee.getRoles().size());
        assertEquals(employee, savedEmployee);
    }

    /**
     * This test is used to verify that the employee repository can't find a non-existing employee by a wrong id.
     */
    @Test
    public void notFoundById() {
        // Execution
        Optional<Employee> foundEmployee = eRepository.findById(999999L);
        // Verification
        log.debug("EMPLOYEE TEST - FIND BY ID - Employee verification");
        assertFalse(foundEmployee.isPresent());
    }

    /**
     * This test is used to verify that the employee repository can say if an employee exists by its email.
     */
    @Test
    public void existsByEmail() {
        // Initialization
        Employee employee = new Employee("existsbyemail@haaga.fi", "syedy0",
                "Sybil", "Yedy", "FAKE PASSWORD", false, true);
        log.debug("EMPLOYEE TEST - EXISTS BY EMAIL - New employee created: " + employee);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(employee);
        em.getTransaction().commit();
        log.debug("EMPLOYEE TEST - EXISTS BY EMAIL - New employee saved: " + employee);
        // Execution
        Boolean result = eRepository.existsByEmail(employee.getEmail());
        // Verification
        log.debug("EMPLOYEE TEST - EXISTS BY EMAIL - Boolean verification");
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * This test is used to verify that the employee repository can say if an employee exists by its email.
     */
    @Test
    public void doesNotExistByEmail() {
        // Execution
        Boolean result = eRepository.existsByEmail("WRONG EMAIL ADDRESS");
        // Verification
        log.debug("EMPLOYEE TEST - DOES NOT EXIST BY EMAIL - Boolean verification");
        assertNotNull(result);
        assertFalse(result);
    }

    /**
     * This test is used to verify that the employee repository can say if an employee exists by its username.
     */
    @Test
    public void existsByUsername() {
        // Initialization
        Employee employee = new Employee("existsbyusername@haaga.fi", "gdumsday1",
                "Guido", "Dumsday", "FAKE PASSWORD", false, true);
        log.debug("EMPLOYEE TEST - EXISTS BY USERNAME - New employee created: " + employee);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(employee);
        em.getTransaction().commit();
        log.debug("EMPLOYEE TEST - EXISTS BY USERNAME - New employee saved: " + employee);
        // Execution
        Boolean result = eRepository.existsByUsername(employee.getUsername());
        // Verification
        log.debug("EMPLOYEE TEST - EXISTS BY EMAIL - Boolean verification");
        assertNotNull(result);
        assertTrue(result);
    }

    /**
     * This test is used to verify that the employee repository can say if an employee exists by its username.
     */
    @Test
    public void doesNotExistByUsername() {
        // Execution
        Boolean result = eRepository.existsByUsername("WRONG USERNAME");
        // Verification
        log.debug("EMPLOYEE TEST - DOES NOT EXIST BY USERNAME - Boolean verification");
        assertNotNull(result);
        assertFalse(result);
    }

    /**
     * This test is used to verify that the employee repository can find a saved employee by his username.
     */
    @Test
    public void findByUsername() {
        // Initialization
        Employee employee = new Employee("findbyusername@haaga.fi", "hobrallaghan2",
                "Haskel", "O'Brallaghan", "FAKE PASSWORD", false, true);
        log.debug("EMPLOYEE TEST - FIND BY USERNAME - New employee created: " + employee);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(employee);
        em.getTransaction().commit();
        log.debug("EMPLOYEE TEST - FIND BY USERNAME - New employee saved: " + employee);
        // Execution
        Optional<Employee> foundEmployee = eRepository.findByUsername(employee.getUsername());
        // Verification
        log.debug("EMPLOYEE TEST - FIND BY USERNAME - Employee verification");
        assertTrue(foundEmployee.isPresent());
        Employee savedEmployee = foundEmployee.get();
        assertNotNull(savedEmployee.getId()); assertEquals(employee.getId(), savedEmployee.getId());
        assertNotNull(savedEmployee.getEmail()); assertEquals(employee.getEmail(), savedEmployee.getEmail());
        assertNotNull(savedEmployee.getUsername()); assertEquals(employee.getUsername(), savedEmployee.getUsername());
        assertNotNull(savedEmployee.getFirstName()); assertEquals(employee.getFirstName(), savedEmployee.getFirstName());
        assertNotNull(savedEmployee.getLastName()); assertEquals(employee.getLastName(), savedEmployee.getLastName());
        assertNotNull(savedEmployee.getPassword()); assertEquals(employee.getPassword(), savedEmployee.getPassword());
        assertNotNull(savedEmployee.getActive()); assertEquals(employee.getActive(), savedEmployee.getActive());
        assertNotNull(savedEmployee.getBlocked()); assertEquals(employee.getBlocked(), savedEmployee.getBlocked());
        assertEquals(employee.getRoles().size(), savedEmployee.getRoles().size());
        assertEquals(employee, savedEmployee);
    }

    /**
     * This test is used to verify that the employee repository can't find a non-existing employee by a wrong username.
     */
    @Test
    public void notFoundByUsername() {
        // Execution
        Optional<Employee> foundEmployee = eRepository.findByUsername("WRONG USERNAME");
        // Verification
        log.debug("EMPLOYEE TEST - FIND BY USERNAME - Employee verification");
        assertFalse(foundEmployee.isPresent());
    }

    /**
     * This test is used to ensure that an
     */
    @Test
    public void deactivateEmployeeById() {
        // Initialization
        Employee employee = new Employee("blockEmployee@haaga.fi", "mflann3",
                "Maryanna", "Flann", "FAKE PASSWORD", true, false);
        log.debug("EMPLOYEE TEST - DEACTIVATE EMPLOYEE BY ID - New employee created: " + employee);
        EntityManager em = testEntityManager.getEntityManager();
        em.persist(employee);
        log.debug("EMPLOYEE TEST - DEACTIVATE EMPLOYEE BY ID - New employee saved: " + employee);
        // Execution
        eRepository.deactivateEmployeeById(employee.getId());
        // Verification
        Optional<Employee> employeeOptional = eRepository.findById(employee.getId());
        assertTrue(employeeOptional.isPresent());
        Employee employeeFound = employeeOptional.get();
        assertNotNull(employeeFound.getActive()); assertEquals(false, employeeFound.getActive());
        assertNotNull(employeeFound.getBlocked()); assertEquals(true, employeeFound.getBlocked());
        assertEquals(employee.getRoles().size(), employeeFound.getRoles().size());
        assertEquals(employee, employeeFound);
    }
}