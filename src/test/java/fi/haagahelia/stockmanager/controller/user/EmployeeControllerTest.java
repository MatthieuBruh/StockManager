package fi.haagahelia.stockmanager.controller.user;

import com.google.gson.Gson;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.user.EmployeeCuDTO;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import fi.haagahelia.stockmanager.repository.user.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class EmployeeControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private String token;

    @BeforeEach
    public void setUp() throws Exception {
        Role admin = new Role("Admin", "For the admins"); roleRepository.save(admin);
        String password = "A1234";
        Employee employee = new Employee("main@haaga.fi", "main", "Main", "Haaga", new BCryptPasswordEncoder().encode(password), true, false);
        employeeRepository.save(employee);
        employee.setRoles(List.of(admin)); employeeRepository.save(employee);
        String requestBody = "{ \"username\": \"" + employee.getUsername() + "\", \"password\": \"" + password + "\"}";
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").accept(MediaType.APPLICATION_JSON).content(requestBody).header("Content-Type", MediaType.APPLICATION_JSON)).andReturn();
        if (mvcResult.getResponse().getStatus() == 200) {
            AuthResponseDTO authResponseDTO = new Gson().fromJson(mvcResult.getResponse().getContentAsString(), AuthResponseDTO.class);
            token = "Bearer " + authResponseDTO.getToken();
        } else {
            throw new RuntimeException("AUTHENTICATION FAILED!!");
        }
    }

    @Test
    public void getEmployees() throws Exception {
        employeeRepository.save(new Employee("john@haaga.fi", "jhndoe", "John", "Doe", new BCryptPasswordEncoder().encode("TEST1234"), true, false));
        employeeRepository.save(new Employee("pierre@haaga.fi", "prrcai", "Pierre", "Caillou", new BCryptPasswordEncoder().encode("TEST1234"), true, false));
        employeeRepository.save(new Employee("jean@haaga.fi", "jndup", "Jean", "Dupont", new BCryptPasswordEncoder().encode("TEST1234"), true, false));
        employeeRepository.save(new Employee("daniel@haaga.fi", "dnlban", "Daniel", "Banane", new BCryptPasswordEncoder().encode("TEST1234"), true, false));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/employees")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.employeeDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.employeeDTOList[*].id").isNotEmpty());
    }

    /* IMPOSSIBLE TO TEST GET ALL EMPLOYEES WITH NO CONTENT DUE TO THE AUTHENTICATED USER */

    @Test
    public void getEmployeeById() throws Exception {
        Employee employee = employeeRepository.save(new Employee("john@haaga.fi", "jhndoe", "John", "Doe", new BCryptPasswordEncoder().encode("TEST1234"), true, false));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/employees/" + employee.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(employee.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("email").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(employee.getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("username").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("username").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("username").value(employee.getUsername()))
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").value(employee.getFirstName()))
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").value(employee.getLastName()))
                .andExpect(MockMvcResultMatchers.jsonPath("isActive").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isActive").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isActive").value(employee.getActive()))
                .andExpect(MockMvcResultMatchers.jsonPath("isBlocked").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isBlocked").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isBlocked").value(employee.getBlocked()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employees.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employees.href").isNotEmpty());
    }

    @Test
    public void getEmployeeById_WrongId() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/employees/" + 999L)
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createEmployee() throws Exception {
        roleRepository.save(new Role("ROLE_VENDOR", "This role is for the vendors."));

        EmployeeCuDTO employeeCuDTO = new EmployeeCuDTO();
        employeeCuDTO.setEmail("john@haaga.fi"); employeeCuDTO.setUsername("jhndoe"); employeeCuDTO.setFirstName("John");
        employeeCuDTO.setLastName("Doe"); employeeCuDTO.setPassword("A1234");
        String requestBody = new Gson().toJson(employeeCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("email").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("email").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(employeeCuDTO.getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("username").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("username").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("username").value(employeeCuDTO.getUsername()))
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").value(employeeCuDTO.getFirstName()))
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").value(employeeCuDTO.getLastName()))
                .andExpect(MockMvcResultMatchers.jsonPath("password").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("password").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isActive").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isActive").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isActive").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("isBlocked").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isBlocked").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isBlocked").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employees.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employees.href").isNotEmpty());
    }

    @Test
    public void createEmployee_NullValues() throws Exception {
        roleRepository.save(new Role("ROLE_VENDOR", "This role is for the vendors."));

        EmployeeCuDTO employeeCuDTO = new EmployeeCuDTO(); String password = "A1234";
        employeeCuDTO.setEmail("john@haaga.fi"); employeeCuDTO.setUsername("jhndoe"); employeeCuDTO.setFirstName("John");
        employeeCuDTO.setLastName("Doe"); employeeCuDTO.setPassword(password);
        String requestBody = new Gson().toJson(employeeCuDTO);

        employeeCuDTO.setEmail(null);
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        employeeCuDTO.setEmail("john@haaga.fi");

        employeeCuDTO.setUsername(null);
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        employeeCuDTO.setUsername("jhndoe");

        employeeCuDTO.setFirstName(null);
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        employeeCuDTO.setFirstName("John");

        employeeCuDTO.setLastName(null);
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        employeeCuDTO.setLastName("Doe");

        employeeCuDTO.setPassword(null);
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createEmployee_EmptyValues() throws Exception {
        roleRepository.save(new Role("ROLE_VENDOR", "This role is for the vendors."));

        EmployeeCuDTO employeeCuDTO = new EmployeeCuDTO(); String password = "A1234";
        employeeCuDTO.setEmail("john@haaga.fi"); employeeCuDTO.setUsername("jhndoe"); employeeCuDTO.setFirstName("John");
        employeeCuDTO.setLastName("Doe"); employeeCuDTO.setPassword(password);
        String requestBody = new Gson().toJson(employeeCuDTO);

        employeeCuDTO.setEmail("");
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        employeeCuDTO.setEmail("john@haaga.fi");

        employeeCuDTO.setUsername("");
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        employeeCuDTO.setUsername("jhndoe");

        employeeCuDTO.setFirstName("");
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        employeeCuDTO.setFirstName("John");

        employeeCuDTO.setLastName("");
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createEmployee_UserNameAlreadyExists() throws Exception {
        roleRepository.save(new Role("ROLE_VENDOR", "This role is for the vendors."));

        EmployeeCuDTO employeeCuDTO = new EmployeeCuDTO();
        employeeCuDTO.setEmail("john@haaga.fi"); employeeCuDTO.setUsername("jhndoe"); employeeCuDTO.setFirstName("John");
        employeeCuDTO.setLastName("Doe"); employeeCuDTO.setPassword("A1234");
        String requestBody = new Gson().toJson(employeeCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated());

        employeeCuDTO.setEmail("john2@haaga.fi");
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void createEmployee_EmailAlreadyExists() throws Exception {
        roleRepository.save(new Role("ROLE_VENDOR", "This role is for the vendors."));

        EmployeeCuDTO employeeCuDTO = new EmployeeCuDTO();
        employeeCuDTO.setEmail("john@haaga.fi"); employeeCuDTO.setUsername("jhndoe"); employeeCuDTO.setFirstName("John");
        employeeCuDTO.setLastName("Doe"); employeeCuDTO.setPassword("A1234");
        String requestBody = new Gson().toJson(employeeCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated());

        employeeCuDTO.setUsername("jhndoe2");
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/employees").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void updateEmployee() throws Exception {
        Role role = new Role("ROLE_VENDOR", "This role is for the vendors."); roleRepository.save(role);
        Employee employee = new Employee("jean@haaga.fi", "jndup", "Jean", "Dupont", new BCryptPasswordEncoder().encode("TEST1234"), false, true);
        employeeRepository.save(employee); employee.setRoles(List.of(role));
        Employee employeeSaved = employeeRepository.save(employee);

        EmployeeCuDTO employeeCuDTO = new EmployeeCuDTO(); employeeCuDTO.setEmail(employeeSaved.getEmail());
        employeeCuDTO.setUsername(employeeSaved.getUsername()); employeeCuDTO.setFirstName("UPDATED FIRST NAME");
        employeeCuDTO.setLastName("UPDATED LAST NAME");
        String requestBody = new Gson().toJson(employeeCuDTO);

        mvc.perform(MockMvcRequestBuilders.put("/api/employees/" + employeeSaved.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(employeeSaved.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("email").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(employeeSaved.getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("username").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("username").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("username").value(employeeSaved.getUsername()))
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").value(employeeCuDTO.getFirstName()))
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").value(employeeCuDTO.getLastName()))
                .andExpect(MockMvcResultMatchers.jsonPath("password").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("password").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isActive").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isActive").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isActive").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("isBlocked").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isBlocked").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isBlocked").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employees.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employees.href").isNotEmpty());
    }

    @Test
    public void updateEmployee_WrongId() throws Exception {
        Role role = new Role("ROLE_VENDOR", "This role is for the vendors."); roleRepository.save(role);
        Employee employee = new Employee("jean@haaga.fi", "jndup", "Jean", "Dupont", new BCryptPasswordEncoder().encode("TEST1234"), false, true);
        employeeRepository.save(employee); employee.setRoles(List.of(role));
        Employee employeeSaved = employeeRepository.save(employee);

        EmployeeCuDTO employeeCuDTO = new EmployeeCuDTO(); employeeCuDTO.setEmail(employeeSaved.getEmail());
        employeeCuDTO.setUsername(employeeSaved.getUsername()); employeeCuDTO.setFirstName("UPDATED FIRST NAME");
        employeeCuDTO.setLastName("UPDATED LAST NAME");
        String requestBody = new Gson().toJson(employeeCuDTO);

        mvc.perform(MockMvcRequestBuilders.put("/api/employees/" + 999L).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateEmployee_NullValues() throws Exception {
        Role role = new Role("ROLE_VENDOR", "This role is for the vendors."); roleRepository.save(role);
        Employee employee = new Employee("jean@haaga.fi", "jndup", "Jean", "Dupont", new BCryptPasswordEncoder().encode("TEST1234"), false, true);
        employeeRepository.save(employee); employee.setRoles(List.of(role));
        Employee employeeSaved = employeeRepository.save(employee);

        EmployeeCuDTO employeeCuDTO = new EmployeeCuDTO(); employeeCuDTO.setEmail(employeeSaved.getEmail());
        employeeCuDTO.setUsername(employeeSaved.getUsername()); employeeCuDTO.setFirstName("UPDATED FIRST NAME");
        employeeCuDTO.setLastName("UPDATED LAST NAME");
        String requestBody = "";

        employeeCuDTO.setEmail(null);
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/employees/" + employeeSaved.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        employeeCuDTO.setEmail(employeeSaved.getEmail());

        employeeCuDTO.setUsername(null);
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/employees/" + employeeSaved.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        employeeCuDTO.setUsername(employeeSaved.getUsername());
    }

    @Test
    public void updateEmployee_EmptyValues() throws Exception {
        Role role = new Role("ROLE_VENDOR", "This role is for the vendors."); roleRepository.save(role);
        Employee employee = new Employee("jean@haaga.fi", "jndup", "Jean", "Dupont", new BCryptPasswordEncoder().encode("TEST1234"), false, true);
        employeeRepository.save(employee); employee.setRoles(List.of(role));
        Employee employeeSaved = employeeRepository.save(employee);

        EmployeeCuDTO employeeCuDTO = new EmployeeCuDTO(); employeeCuDTO.setEmail(employeeSaved.getEmail());
        employeeCuDTO.setUsername(employeeSaved.getUsername()); employeeCuDTO.setFirstName("UPDATED FIRST NAME");
        employeeCuDTO.setLastName("UPDATED LAST NAME");
        String requestBody = "";

        employeeCuDTO.setEmail("");
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/employees/" + employeeSaved.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        employeeCuDTO.setEmail(employeeSaved.getEmail());

        employeeCuDTO.setUsername("");
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/employees/" + employeeSaved.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        employeeCuDTO.setUsername(employeeSaved.getUsername());
    }

    @Test
    public void updateEmployee_NotFound() throws Exception {
        Role role = new Role("ROLE_VENDOR", "This role is for the vendors."); roleRepository.save(role);
        Employee employee = new Employee("jean@haaga.fi", "jndup", "Jean", "Dupont", new BCryptPasswordEncoder().encode("TEST1234"), false, true);
        employeeRepository.save(employee); employee.setRoles(List.of(role));
        Employee employeeSaved = employeeRepository.save(employee);

        EmployeeCuDTO employeeCuDTO = new EmployeeCuDTO(); employeeCuDTO.setEmail(employeeSaved.getEmail());
        employeeCuDTO.setUsername(employeeSaved.getUsername()); employeeCuDTO.setFirstName("UPDATED FIRST NAME");
        employeeCuDTO.setLastName("UPDATED LAST NAME");
        String requestBody = "";

        employeeCuDTO.setEmail("wrong@email.com");
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/employees/" + employeeSaved.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        employeeCuDTO.setEmail(employeeSaved.getEmail());

        employeeCuDTO.setUsername("wrongUsername");
        requestBody = new Gson().toJson(employeeCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/employees/" + employeeSaved.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        employeeCuDTO.setUsername(employeeSaved.getUsername());
    }

    @Test
    public void activateEmployee() throws Exception {
        Role role = new Role("ROLE_VENDOR", "This role is for the vendors."); roleRepository.save(role);
        Employee employee = new Employee("jean@haaga.fi", "jndup", "Jean", "Dupont", new BCryptPasswordEncoder().encode("TEST1234"), false, true);
        employeeRepository.save(employee); employee.setRoles(List.of(role));
        Employee employeeSaved = employeeRepository.save(employee);

        mvc.perform(MockMvcRequestBuilders.put("/api/employees/" + employeeSaved.getId() + "/activate").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(employeeSaved.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("email").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(employeeSaved.getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("username").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("username").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("username").value(employeeSaved.getUsername()))
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").value(employeeSaved.getFirstName()))
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").value(employeeSaved.getLastName()))
                .andExpect(MockMvcResultMatchers.jsonPath("password").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("password").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isActive").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isActive").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isActive").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("isBlocked").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isBlocked").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isBlocked").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employees.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employees.href").isNotEmpty());
    }

    @Test
    public void activateEmployee_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put("/api/employees/" + 99L + "/activate").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteEmployeeById() throws Exception {
        Role role = new Role("ROLE_VENDOR", "This role is for the vendors."); roleRepository.save(role);
        Employee employee = new Employee("jean@haaga.fi", "jndup", "Jean", "Dupont", new BCryptPasswordEncoder().encode("TEST1234"), false, true);
        employeeRepository.save(employee); employee.setRoles(List.of(role));
        Employee employeeSaved = employeeRepository.save(employee);

        mvc.perform(MockMvcRequestBuilders.delete("/api/employees/" + employeeSaved.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteEmployeeById_WrongId() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/employees/" + 99L).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }
}