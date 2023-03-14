package fi.haagahelia.stockmanager.controller.customer.order;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrderLine;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderLineRepository;
import fi.haagahelia.stockmanager.repository.product.BrandRepository;
import fi.haagahelia.stockmanager.repository.product.CategoryRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import fi.haagahelia.stockmanager.repository.supplier.SupplierRepository;
import fi.haagahelia.stockmanager.tools.LocalDateAdapter;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.customer.order.CustomerOrderCuDTO;
import fi.haagahelia.stockmanager.model.customer.Customer;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.customer.CustomerRepository;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderRepository;
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

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CustomerOrderControllerTest {

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
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").accept(MediaType.APPLICATION_JSON).content(requestBody).header("Content-Type", MediaType.APPLICATION_JSON)).andReturn();
        if (mvcResult.getResponse().getStatus() == 200) {
            AuthResponseDTO authResponseDTO = new Gson().fromJson(mvcResult.getResponse().getContentAsString(), AuthResponseDTO.class);
            token = "Bearer " + authResponseDTO.getToken();
        } else {
            throw new RuntimeException("AUTHENTICATION FAILED!!");
        }
    }

    @Test
    public void getCustomerOrders() throws Exception {
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(4), false, employee, null));
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(5), false, employee, null));
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, null));
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(2), false, employee, null));
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(10), false, employee, null));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/orders")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.customerOrderDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.customerOrderDTOList[*].id").isNotEmpty());
    }

    @Test
    public void getCustomerOrders_NoContent() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/orders")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.customerOrderDTOList").doesNotExist());
    }

    @Test
    public void getCustomerOrder() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(5), false, employee, customer));
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/orders/" + customerOrder.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(customerOrder.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(customerOrder.getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").value(customerOrder.getDeliveryDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").value(customerOrder.getSent()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer-orders.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employee.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employee.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-customer-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-customer-orders.href").isNotEmpty());
    }

    @Test
    public void getCustomerOrder_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/orders/" + 999L)
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetCustomerOrders() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(5), false, employee, customer));
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, null));
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(2), false, employee, customer));
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(10), false, employee, null));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/" + customer.getId() + "/orders")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.customerOrderDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.customerOrderDTOList[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.customerOrderDTOList").isArray());
    }

    @Test
    public void testGetCustomerOrders_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/" + 999L + "/orders")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getCustOrdersDate() throws Exception {
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(5), false, employee, null));
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, null));
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(2), false, employee, null));
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(5), false, employee, null));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/orders/delivery=" + LocalDate.now().plusDays(5).toString())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.customerOrderDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.customerOrderDTOList[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.customerOrderDTOList").isArray());

    }

    @Test
    public void getCustOrdersDate_NoContent() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/customers/orders/delivery=" + LocalDate.now().plusDays(30))
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void createCustomerOrder() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        CustomerOrderCuDTO customerOrderCuDTO = new CustomerOrderCuDTO(LocalDate.now(), LocalDate.now().plusDays(22), false, employee.getId(), customer.getId());
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        String requestBody = gson.toJson(customerOrderCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/customers/orders").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(customerOrderCuDTO.getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").value(customerOrderCuDTO.getDeliveryDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").value(customerOrderCuDTO.getIsSent()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer-orders.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employee.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employee.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-customer-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-customer-orders.href").isNotEmpty());
    }

    @Test
    public void createCustomerOrder_NullDates() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        CustomerOrderCuDTO customerOrderCuDTO = new CustomerOrderCuDTO(LocalDate.now(), LocalDate.now().plusDays(22), false, employee.getId(), customer.getId());
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        String requestBody;

        customerOrderCuDTO.setDate(null);
        requestBody = gson.toJson(customerOrderCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/customers/orders").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        customerOrderCuDTO.setDate(LocalDate.now());

        customerOrderCuDTO.setDeliveryDate(null);
        requestBody = gson.toJson(customerOrderCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/customers/orders").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        customerOrderCuDTO.setDeliveryDate(LocalDate.now().plusDays(22));
    }

    @Test
    public void updateOrder() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(5), false, employee, customer));

        CustomerOrderCuDTO customerOrderCuDTO = new CustomerOrderCuDTO(customerOrder.getDate(), customerOrder.getDeliveryDate(), customerOrder.getSent(), customerOrder.getEmployee().getId(), customerOrder.getCustomer().getId());
        customerOrderCuDTO.setDeliveryDate(LocalDate.now().plusDays(20));
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        String requestBody = gson.toJson(customerOrderCuDTO);

        mvc.perform(MockMvcRequestBuilders.put("/api/customers/orders/" + customerOrder.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(customerOrder.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(customerOrder.getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").value(customerOrderCuDTO.getDeliveryDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").value(customerOrder.getSent()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer-orders.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employee.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employee.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-customer-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-customer-orders.href").isNotEmpty());
    }

    @Test
    public void updateOrder_BadRequest() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(5), false, employee, customer));

        CustomerOrderCuDTO customerOrderCuDTO = new CustomerOrderCuDTO(customerOrder.getDate(), customerOrder.getDeliveryDate(), customerOrder.getSent(), customerOrder.getEmployee().getId(), customerOrder.getCustomer().getId());
        customerOrderCuDTO.setDeliveryDate(LocalDate.now().plusDays(20));
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        String requestBody = gson.toJson(customerOrderCuDTO);

        mvc.perform(MockMvcRequestBuilders.put("/api/customers/orders/" + 999L).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void sendOrder() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category category = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand brand = brandRepository.save(new Brand("AMD"));
        Supplier supplier = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, brand, category, supplier));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(3, 340.0, customerOrder, product));

        mvc.perform(MockMvcRequestBuilders.put("/api/customers/orders/" + customerOrder.getId() + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(customerOrder.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(customerOrder.getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").value(customerOrder.getDeliveryDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer-orders.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employee.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employee.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-customer-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-customer-orders.href").isNotEmpty());
    }

    @Test
    public void sendOrder_NoOrderLines() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(5), false, employee, customer));

        mvc.perform(MockMvcRequestBuilders.put("/api/customers/orders/" + customerOrder.getId() + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    public void sendOrder_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put("/api/customers/orders/" + 999L + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void sendOrder_AlreadySent() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(5), true, employee, customer));

        mvc.perform(MockMvcRequestBuilders.put("/api/customers/orders/" + customerOrder.getId() + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void sendOrder_NotEnoughStock() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category category = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand brand = brandRepository.save(new Brand("AMD"));
        Supplier supplier = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 2, 10, 3, brand, category, supplier));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(3, 340.0, customerOrder, product));

        mvc.perform(MockMvcRequestBuilders.put("/api/customers/orders/" + customerOrder.getId() + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotModified());
    }

    @Test
    public void cancelSendOrder() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category category = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand brand = brandRepository.save(new Brand("AMD"));
        Supplier supplier = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, brand, category, supplier));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), true, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(3, 340.0, customerOrder, product));

        mvc.perform(MockMvcRequestBuilders.put("/api/customers/orders/" + customerOrder.getId() + "/cancel-sending").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(customerOrder.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(customerOrder.getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").value(customerOrder.getDeliveryDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isSent").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer-orders.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employee.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employee.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.customer.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-customer-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-customer-orders.href").isNotEmpty());
    }

    @Test
    public void cancelSendOrder_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put("/api/customers/orders/" + 99L + "/cancel-sending").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void cancelSendOrder_NotSent() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(5), false, employee, customer));

        mvc.perform(MockMvcRequestBuilders.put("/api/customers/orders/" + customerOrder.getId() + "/cancel-sending").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void deleteOrder() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(5), false, employee, customer));

        mvc.perform(MockMvcRequestBuilders.delete("/api/customers/orders/" + customerOrder.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteOrder_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/customers/orders/" + 99L).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteOrder_PreconditionFailed() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now().minusDays(4), LocalDate.now().plusDays(5), false, employee, customer));

        mvc.perform(MockMvcRequestBuilders.delete("/api/customers/orders/" + customerOrder.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    public void deleteOrderForce() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now().plusDays(4), LocalDate.now().plusDays(10), false, employee, customer));

        mvc.perform(MockMvcRequestBuilders.delete("/api/customers/orders/" + customerOrder.getId() + "/force").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteOrderForce_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/customers/orders/" + 99L + "/force").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }
}