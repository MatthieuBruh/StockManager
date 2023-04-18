package fi.haagahelia.stockmanager.controller.customer;

import com.google.gson.Gson;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.customer.CustomerCuDTO;
import fi.haagahelia.stockmanager.model.common.Geolocation;
import fi.haagahelia.stockmanager.model.customer.Customer;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.common.GeolocationRepository;
import fi.haagahelia.stockmanager.repository.customer.CustomerRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CustomerControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private GeolocationRepository geoRepository;

    private String token;

    @BeforeEach
    public void setUp() throws Exception {
        Role admin = new Role("ROLE_ADMIN", "For the admins"); roleRepository.save(admin);
        String password = "A1234";
        Employee employee = new Employee("main@haaga.fi", "main", "Main", "Haaga", new BCryptPasswordEncoder().encode(password), true, false);
        employeeRepository.save(employee);
        employee.setRoles(List.of(admin)); employeeRepository.save(employee);
        String requestBody = "{ \"username\": \"" + employee.getUsername() + "\", \"password\": \"" + password + "\"}";
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").accept(MediaType.APPLICATION_JSON).content(requestBody).header("Content-Type", MediaType.APPLICATION_JSON).with(csrf())).andReturn();
        if (mvcResult.getResponse().getStatus() == 200) {
            AuthResponseDTO authResponseDTO = new Gson().fromJson(mvcResult.getResponse().getContentAsString(), AuthResponseDTO.class);
            token = "Bearer " + authResponseDTO.getToken();
        } else {
            throw new RuntimeException("AUTHENTICATION FAILED!!");
        }
    }

    @Test
    public void getCustomers() throws Exception {
        customerRepository.save(new Customer("John", "Doe", "johndoe@gmail.com", null));
        customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        customerRepository.save(new Customer("Yves", "Remord", "yvesremord@gmail.com", null));
        customerRepository.save(new Customer("Jean", "Lasalle", "jeanLasalle@gmail.com", null));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.customerDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.customerDTOList[*].id").isNotEmpty());
    }

    @Test
    public void getCustomers_NoContent() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    public void getCustomer() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/" + customer.getEmail())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").value(customer.getFirstName()))
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").value(customer.getLastName()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("email").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(customer.getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customers.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customers.href").isNotEmpty());
    }

    @Test
    public void getCustomer_WrongEmail() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/" + "nothing@outlook.com")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createCustomer() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Ratapihantie", "13", "00250", "Helsinki", "Finland"));

        CustomerCuDTO customerCuDTO = new CustomerCuDTO(); customerCuDTO.setEmail("paul@mirabel.com");
        customerCuDTO.setFirstName("Paul"); customerCuDTO.setLastName("Mirabel"); customerCuDTO.setGeolocationId(geo.getId());
        String requestBody = new Gson().toJson(customerCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/customers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").value(customerCuDTO.getFirstName()))
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").value(customerCuDTO.getLastName()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("email").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(customerCuDTO.getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customers.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customers.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.geolocation.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.geolocation.href").isNotEmpty());
    }

    @Test
    public void createCustomer_WithoutGeo() throws Exception {

        CustomerCuDTO customerCuDTO = new CustomerCuDTO(); customerCuDTO.setEmail("paul@mirabel.com");
        customerCuDTO.setFirstName("Paul"); customerCuDTO.setLastName("Mirabel"); customerCuDTO.setGeolocationId(null);
        String requestBody = new Gson().toJson(customerCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/customers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").value(customerCuDTO.getFirstName()))
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").value(customerCuDTO.getLastName()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("email").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(customerCuDTO.getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customers.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customers.href").isNotEmpty());
    }

    @Test
    public void createCustomer_WrongValues() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Ratapihantie", "13", "00250", "Helsinki", "Finland"));

        CustomerCuDTO customerCuDTO = new CustomerCuDTO(); customerCuDTO.setEmail("paul@mirabel.com");
        customerCuDTO.setFirstName("Paul"); customerCuDTO.setLastName("Mirabel"); customerCuDTO.setGeolocationId(geo.getId());
        String requestBody;

        // Null Values

        customerCuDTO.setEmail(null);
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/customers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        customerCuDTO.setEmail("paul@mirabel.com");

        customerCuDTO.setFirstName(null);
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/customers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        customerCuDTO.setFirstName("Paul");

        customerCuDTO.setLastName(null);
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/customers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        customerCuDTO.setLastName("Mirabel");

        // Empty Values

        customerCuDTO.setEmail("");
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/customers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        customerCuDTO.setEmail("paul@mirabel.com");

        customerCuDTO.setFirstName("");
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/customers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        customerCuDTO.setFirstName("Paul");

        customerCuDTO.setLastName("");
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/customers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        customerCuDTO.setLastName("Mirabel");

        // Wrong Geolocation ID
        customerCuDTO.setGeolocationId(999L);
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/customers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateCustomer() throws Exception {
        Customer customer = customerRepository.save(new Customer("George", "Clooney", "george@clooney.com", null));

        CustomerCuDTO customerCuDTO = new CustomerCuDTO(); customerCuDTO.setEmail(customer.getEmail());
        customerCuDTO.setFirstName("UPDATED FIRST NAME"); customerCuDTO.setLastName("UPDATED LAST NAME");
        customerCuDTO.setGeolocationId(null); String requestBody = new Gson().toJson(customerCuDTO);

        mvc.perform(MockMvcRequestBuilders.put("/api/customers/" + customer.getEmail()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("firstName").value(customerCuDTO.getFirstName()))
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("lastName").value(customerCuDTO.getLastName()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("email").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(customer.getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customers.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customers.href").isNotEmpty());

    }

    @Test
    public void updateCustomer_WrongData() throws Exception {
        Customer customer = customerRepository.save(new Customer("George", "Clooney", "george@clooney.com", null));

        CustomerCuDTO customerCuDTO = new CustomerCuDTO(); customerCuDTO.setEmail(customer.getEmail());
        customerCuDTO.setFirstName(customer.getFirstName()); customerCuDTO.setLastName(customer.getLastName());
        customerCuDTO.setGeolocationId(null);
        String requestBody;

        // Null Values

        customerCuDTO.setEmail(null);
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/customers/" + customer.getEmail()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        customerCuDTO.setEmail(customer.getEmail());

        customerCuDTO.setFirstName(null);
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/customers/" + customer.getEmail()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        customerCuDTO.setFirstName(customer.getFirstName());

        customerCuDTO.setLastName(null);
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/customers/" + customer.getEmail()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        customerCuDTO.setLastName(customer.getLastName());
        // =============================================================================================================

        // Empty Values

        customerCuDTO.setEmail("");
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/customers/" + customer.getEmail()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        customerCuDTO.setEmail(customer.getEmail());

        customerCuDTO.setFirstName("");
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/customers/" + customer.getEmail()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        customerCuDTO.setFirstName(customer.getFirstName());

        customerCuDTO.setLastName("");
        requestBody = new Gson().toJson(customerCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/customers/" + customer.getEmail()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        customerCuDTO.setLastName(customer.getLastName());
    }

    @Test
    public void deleteCustomer() throws Exception {
        Customer customer = customerRepository.save(new Customer("George", "Clooney", "george@clooney.com", null));

        mvc.perform(MockMvcRequestBuilders.delete("/api/customers/" + customer.getEmail()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteCustomer_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/customers/" + 99L).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
    }
}