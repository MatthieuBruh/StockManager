package fi.haagahelia.stockmanager.service;

import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import fi.haagahelia.stockmanager.repository.user.RoleRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Log4j2
public class CustomEmployeeDetailsServiceTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Mock
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @InjectMocks
    private CustomEmployeeDetailsService employeeDetailsService;


    @BeforeEach
    public void setUp() {
        EntityManager em = testEntityManager.getEntityManager();
        em.createQuery("DELETE Employee ").executeUpdate();
        em.createQuery("DELETE Role ").executeUpdate();
        log.info("CUSTOM EMPLOYEE DETAILS SERVICE TEST - INIT - DATABASE CLEARED.");
    }

    @Test
    public void loadUserByUsernameNotFound() {
        // Arrange
        String username = "nonexistentuser";
        when(employeeRepository.findByUsername(username)).thenReturn(Optional.empty());
        log.info("CUSTOM EMPLOYEE DETAILS SERVICE TEST - LOAD USER BY USERNAME NOT FOUND - Execution done.");
        // Act & Assert
        assertThrows(
                UsernameNotFoundException.class,
                () -> employeeDetailsService.loadUserByUsername(username),
                "No employee found with the username: " + username
        );
        log.info("CUSTOM EMPLOYEE DETAILS SERVICE TEST - LOAD USER BY USERNAME NOT FOUND - Verification done.");
    }

    @Test
    public void loadUserByUsernameFound() {
        // Arrange
        Role role = new Role("ROLE_TEST", "This is a test for role");
        Role savedRole = roleRepository.save(role);
        log.info("CUSTOM EMPLOYEE DETAILS SERVICE TEST - LOAD USER BY USERNAME FOUND - New role saved: " + role);

        Employee employee = new Employee("testing@haaga-helia.fi", "jhn", "John", "Doe", new BCryptPasswordEncoder().encode("1234"), true, false);
        employee.addRole(savedRole);
        employeeRepository.save(employee);
        log.info("CUSTOM EMPLOYEE DETAILS SERVICE TEST - LOAD USER BY USERNAME FOUND - New employee saved: " + employee);

        // Execution
        String username = employee.getUsername();
        when(employeeRepository.findByUsername(username)).thenReturn(Optional.of(employee));
        UserDetails userDetails = employeeDetailsService.loadUserByUsername(username);
        log.info("CUSTOM EMPLOYEE DETAILS SERVICE TEST - LOAD USER BY USERNAME FOUND - Execution done.");

        // Verifications
        assertEquals(username, userDetails.getUsername());
        assertEquals(employee.getPassword(), userDetails.getPassword());
        assertEquals(employee.getRoles().size(), userDetails.getAuthorities().size());
        log.info("CUSTOM EMPLOYEE DETAILS SERVICE TEST - LOAD USER BY USERNAME FOUND - Verifications done.");
    }
}