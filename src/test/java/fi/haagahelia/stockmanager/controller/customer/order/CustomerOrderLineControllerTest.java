package fi.haagahelia.stockmanager.controller.customer.order;

import com.google.gson.Gson;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.customer.order.CustomerOrderLineCuDTO;
import fi.haagahelia.stockmanager.model.customer.Customer;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrderLine;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.customer.CustomerRepository;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderLineRepository;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderRepository;
import fi.haagahelia.stockmanager.repository.product.BrandRepository;
import fi.haagahelia.stockmanager.repository.product.CategoryRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import fi.haagahelia.stockmanager.repository.supplier.SupplierRepository;
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

import java.time.LocalDate;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CustomerOrderLineControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private CustomerOrderLineRepository customerOrderLineRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    private Employee employee;

    private String token;

    @BeforeEach
    public void setUp() throws Exception {
        Role admin = new Role("ROLE_ADMIN", "For the admins"); roleRepository.save(admin);
        String password = "A1234";
        employee = new Employee("main@haaga.fi", "main", "Main", "Haaga", new BCryptPasswordEncoder().encode(password), true, false);
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
    public void getOrderLines() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Brand intel = brandRepository.save(new Brand("Intel"));
        Supplier yata = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));

        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, yata));
        Product i9 = productRepository.save(new Product("Core i9-12900K", "empty", 430.40, 445.0, 20, 2, 10, intel, processor, yata));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(3, 340.0, customerOrder, z690f));
        customerOrderLineRepository.save(new CustomerOrderLine(3, 340.0, customerOrder, i9));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/orders/order=" + customerOrder.getId() + "/details")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.customerOrderLineDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty());
    }

    @Test
    public void getOrderLines_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/orders/order=" + 999L + "/details")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getCusOrderLine() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Supplier yata = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, yata));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(3, 340.0, customerOrder, z690f));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + z690f.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("quantity").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("quantity").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("sellPrice").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("sellPrice").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.product.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.product.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.order.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.order.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.order-details.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.order-details.href").isNotEmpty());
    }

    @Test
    public void getCusOrderLine_BadRequest() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Supplier yata = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, yata));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(3, 340.0, customerOrder, z690f));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/orders/order=" + 999L + "/details/product=" + z690f.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isBadRequest());

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + 999L)
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getCusOrderLine_NoContent() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Supplier yata = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, yata));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + z690f.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    public void createCusOrderLine() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 40, 10, 20, brand, category, supplier));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        CustomerOrderLineCuDTO customerOrderLineCuDTO = new CustomerOrderLineCuDTO(10, 350.50);
        String requestBody = new Gson().toJson(customerOrderLineCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("quantity").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("quantity").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("sellPrice").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("sellPrice").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.product.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.product.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.order.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.order.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.order-details.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.order-details.href").isNotEmpty());
    }

    @Test
    public void createCusOrderLine_BadRequest() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 40, 10, 20, brand, category, supplier));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        CustomerOrderLineCuDTO customerOrderLineCuDTO = new CustomerOrderLineCuDTO(10, 350.50);
        String requestBody = new Gson().toJson(customerOrderLineCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/customers/orders/order=" + 999L + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());

        mvc.perform(MockMvcRequestBuilders.post("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + 888L).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createCusOrderLine_DeliveryDatePassed() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 40, 10, 20, brand, category, supplier));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().minusDays(7), false, employee, customer));
        CustomerOrderLineCuDTO customerOrderLineCuDTO = new CustomerOrderLineCuDTO(10, 350.50);
        String requestBody = new Gson().toJson(customerOrderLineCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    public void createCusOrderLine_NotEnoughStock() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 40, 10, 20, brand, category, supplier));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        CustomerOrderLineCuDTO customerOrderLineCuDTO = new CustomerOrderLineCuDTO(100, 350.50);
        String requestBody = new Gson().toJson(customerOrderLineCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    public void createCusOrderLine_OtherError() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 40, 10, 20, brand, category, supplier));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        CustomerOrderLineCuDTO customerOrderLineCuDTO = new CustomerOrderLineCuDTO(10, 350.50);
        String requestBody;

        // Wrong quantity
        customerOrderLineCuDTO.setQuantity(-100);
        requestBody = new Gson().toJson(customerOrderLineCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isUnprocessableEntity());
        customerOrderLineCuDTO.setQuantity(10);

        // Command line already exists
        requestBody = new Gson().toJson(customerOrderLineCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isCreated());

        mvc.perform(MockMvcRequestBuilders.post("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    public void deleteOrderLine() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, brand, category, supplier));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(3, 340.0, customerOrder, product));

        mvc.perform(MockMvcRequestBuilders.delete("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteOrderLine_Wrong() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, brand, category, supplier));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), true, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(3, 340.0, customerOrder, product));

        // Bad request
        mvc.perform(MockMvcRequestBuilders.delete("/api/customers/orders/order=" + 999L + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());

        mvc.perform(MockMvcRequestBuilders.delete("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + 88L).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());

        // Already sent
        mvc.perform(MockMvcRequestBuilders.delete("/api/customers/orders/order=" + customerOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isPreconditionFailed());
    }
}