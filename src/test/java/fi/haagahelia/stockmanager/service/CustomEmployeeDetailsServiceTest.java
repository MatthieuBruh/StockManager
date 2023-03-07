package fi.haagahelia.stockmanager.service;

import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Log4j2
public class CustomEmployeeDetailsServiceTest {

    @Mock
    private EmployeeRepository employeeRepositoryMock;

    @Autowired
    private TestEntityManager testEntityManager;

    @InjectMocks
    private CustomEmployeeDetailsService customEmployeeDetailsService;

    private Employee employee;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        customEmployeeDetailsService = new CustomEmployeeDetailsService(employeeRepositoryMock);

        EntityManager em = testEntityManager.getEntityManager();
        em.createQuery("DELETE Employee").executeUpdate();
        em.createQuery("DELETE Role").executeUpdate();
        log.info("CUSTOM EMPLOYEE DETAILS SERVICE - INIT - DATABASE CLEARED.");

        Role role = new Role("ROLE_TEST", "This is a test for role");
        em.persist(role);
        log.info("CUSTOM EMPLOYEE DETAILS SERVICE - INIT - New role saved: {}.", role);

        employee = new Employee("testing@haaga-helia.fi", "jhn", "John", "Doe", new BCryptPasswordEncoder().encode("1234"), true, false);
        employee.addRole(role);
        em.persist(employee);
        log.info("CUSTOM EMPLOYEE DETAILS SERVICE - INIT - New employee saved: {}.", employee);
        em.getTransaction().commit();
    }

    @Test
    public void testLoadUserByUsername() {
        /*
        // Arrange
        String username = "testuser";
        Employee employee = new Employee();
        employee.setUsername(username);
        when(employeeRepositoryMock.findByUsername(username)).thenReturn(Optional.of(employee));

        // Act
        UserDetails userDetails = customEmployeeDetailsService.loadUserByUsername(username);

        // Assert
        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());
        UserDetails foundEmployee = customEmployeeDetailsService.loadUserByUsername(employee.getUsername());
        log.info("CUSTOM EMPLOYEE DETAILS SERVICE - LOAD BY USERNAME - EXECUTION DONE.");
        assertNotNull(foundEmployee);
        assertEquals(employee, foundEmployee);
        log.info("CUSTOM EMPLOYEE DETAILS SERVICE - LOAD BY USERNAME - EXECUTION DONE.");
        */
    }

    @Test
    public void testLoadUserByUsernameThrowsExceptionWhenUserNotFound() {
        /*
        // Arrange
        String username = "testuser";
        when(employeeRepositoryMock.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            customEmployeeDetailsService.loadUserByUsername(username);
        });
        */
    }

    @Test
    public void testLoadUserByUsernameThrowsRuntimeExceptionWhenRepositoryThrowsException() {
        /*
        // Arrange
        String username = "testuser";
        when(employeeRepositoryMock.findByUsername(username)).thenThrow(new RuntimeException());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            customEmployeeDetailsService.loadUserByUsername(username);
        });
        */
    }
}