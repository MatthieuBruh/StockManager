package fi.haagahelia.stockmanager.controller.user;

import com.google.gson.Gson;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.model.customer.Customer;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrderLine;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLine;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.customer.CustomerRepository;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderLineRepository;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderRepository;
import fi.haagahelia.stockmanager.repository.product.BrandRepository;
import fi.haagahelia.stockmanager.repository.product.CategoryRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import fi.haagahelia.stockmanager.repository.supplier.SupplierRepository;
import fi.haagahelia.stockmanager.repository.supplier.order.SupplierOrderLineRepository;
import fi.haagahelia.stockmanager.repository.supplier.order.SupplierOrderRepository;
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class StatisticsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private SupplierOrderRepository supplierOrderRepository;

    @Autowired
    private SupplierOrderLineRepository supplierOrderLineRepository;

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
    public void suppliersStats() throws Exception {
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Brand intel = brandRepository.save(new Brand("Intel"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));

        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, supplier));
        Product i9 = productRepository.save(new Product("Core i9-12900K", "empty", 430.40, 445.0, 20, 2, 10, intel, processor, supplier));


        Supplier samsung = supplierRepository.save(new Supplier("Samsung", "supplier@samsung.com", "", null));
        SupplierOrder supplierOrder1 = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(2), false, false, samsung));

        Supplier einti = supplierRepository.save(new Supplier("Einti", "supplier@einti.com", "843247447", null));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, einti));

        Supplier jaloo = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        SupplierOrder supplierOrder2 = supplierOrderRepository.save(new SupplierOrder(LocalDate.now().plusDays(31), LocalDate.now().plusDays(40), false, false, jaloo));

        Supplier eare = supplierRepository.save(new Supplier("Eare", "supplier@eare.com", "32343422", null));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now().plusDays(59), LocalDate.now().plusDays(63), false, false, eare));

        supplierOrderLineRepository.save(new SupplierOrderLine(3, 340.0, supplierOrder1, z690f));
        supplierOrderLineRepository.save(new SupplierOrderLine(3, 340.0, supplierOrder2, i9));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/suppliers")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(LocalDate.now().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrders").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrders").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrders").value(4))
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrdersForTheMonth").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrdersForTheMonth").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrdersForTheMonth").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("orderValuesForTheMonth").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("orderValuesForTheMonth").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("orderValuesForTheMonth").value(3 * 340.0))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty());
    }

    @Test
    public void suppliersStats_NoContent() throws Exception {

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/suppliers")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void customersStats() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Brand intel = brandRepository.save(new Brand("Intel"));
        Supplier yata = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));

        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, yata));
        Product i9 = productRepository.save(new Product("Core i9-12900K", "empty", 430.40, 445.0, 20, 2, 10, intel, processor, yata));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(2, 340.0, customerOrder, z690f));

        CustomerOrder customerOrder2 = customerOrderRepository.save(new CustomerOrder(LocalDate.now().plusDays(31), LocalDate.now().plusDays(7), false, employee, null));
        customerOrderLineRepository.save(new CustomerOrderLine(5, 340.0, customerOrder2, i9));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/customers")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(LocalDate.now().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrders").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrders").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrders").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrdersForTheMonth").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrdersForTheMonth").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrdersForTheMonth").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("orderValuesForTheMonth").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("orderValuesForTheMonth").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("orderValuesForTheMonth").value(2 * 340.0))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty());
    }

    @Test
    public void customersStats_NoContent() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/customers")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void salesPerVendor() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Brand intel = brandRepository.save(new Brand("Intel"));
        Supplier yata = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));

        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, yata));
        Product i9 = productRepository.save(new Product("Core i9-12900K", "empty", 430.40, 445.0, 20, 2, 10, intel, processor, yata));


        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(2, 340.0, customerOrder, z690f));

        CustomerOrder customerOrder2 = customerOrderRepository.save(new CustomerOrder(LocalDate.now().plusDays(31), LocalDate.now().plusDays(7), false, employee, null));
        customerOrderLineRepository.save(new CustomerOrderLine(5, 340.0, customerOrder2, i9));

        Employee second = new Employee("jhnd@haaga.fi", "jhnd", "Doe", "Doe", new BCryptPasswordEncoder().encode("AQ23"), true, false);
        employeeRepository.save(second);
        customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, second, customer));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/employee=" + employee.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(LocalDate.now().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrders").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrders").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrders").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrdersForTheMonth").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrdersForTheMonth").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("totalOrdersForTheMonth").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("orderValuesForTheMonth").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("orderValuesForTheMonth").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("orderValuesForTheMonth").value(2 * 340.0))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employee.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.employee.href").isNotEmpty());
    }

    @Test
    public void salesPerVendor_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/employee=" + 999L)
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void salesPerVendor_NoContent() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/employee=" + employee.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void stockToSalesRatio() throws Exception {
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Supplier yata = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));

        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, yata));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(2, 340.0, customerOrder, z690f));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/stock-to-sale-ratio")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("resultName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("resultName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("resultName").value("Stock to sale ratio"))
                .andExpect(MockMvcResultMatchers.jsonPath("resultValue").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("resultValue").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("resultValue").value(2.2823529411764705)) // = 1552 / 680
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty());
    }

    @Test
    public void stockToSalesRatio_NoContent() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/stock-to-sale-ratio")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void sellThroughRate() throws Exception {
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));

        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, supplier));

        Supplier samsung = supplierRepository.save(new Supplier("Samsung", "supplier@samsung.com", "", null));
        SupplierOrder supplierOrder1 = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(2), true, true, samsung));

        supplierOrderLineRepository.save(new SupplierOrderLine(3, 320.0, supplierOrder1, z690f));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(2, 340.0, customerOrder, z690f));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/sell-through-rate")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("resultName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("resultName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("resultName").value("Sell-through rate"))
                .andExpect(MockMvcResultMatchers.jsonPath("resultValue").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("resultValue").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("resultValue").value((2/3) * 100))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty());
    }

    @Test
    public void sellThroughRate_Errors() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/sell-through-rate")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());


        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));

        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, supplier));

        Supplier samsung = supplierRepository.save(new Supplier("Samsung", "supplier@samsung.com", "", null));
        SupplierOrder supplierOrder1 = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(2), true, false, samsung));

        supplierOrderLineRepository.save(new SupplierOrderLine(3, 320.0, supplierOrder1, z690f));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(2, 340.0, customerOrder, z690f));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/sell-through-rate")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    public void stockOuts() throws Exception {
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Category graphicCard = categoryRepository.save(new Category("Graphic Cards", "For the graphic cards"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Brand intel = brandRepository.save(new Brand("Intel"));
        Brand amd = brandRepository.save(new Brand("AMD"));
        Supplier yata = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Supplier midel = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));

        productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 20, 10, 20, asus, motherboard, yata));
        productRepository.save(new Product("ROG CROSSHAIR VIII DARK HERO", "empty", 300.15, 319.50, 0, 5, 30, asus, motherboard, yata));
        productRepository.save(new Product("Core i9-12900K", "empty", 430.40, 445.0, null, 2, 10, intel, processor, yata));
        productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 5, 10, 3, amd, processor, midel));
        productRepository.save(new Product("Graphique Arc A750", "empty", 250.75, 301.10, 10, 4, 20, intel, graphicCard, midel));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/stock-outs")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("resultName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("resultName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("resultName").value("Stock-outs"))
                .andExpect(MockMvcResultMatchers.jsonPath("resultValue").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("resultValue").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("resultValue").value(1/4))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty());
    }

    @Test
    public void stockOuts_Error() throws Exception {
        // Error
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/stock-outs")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Precondition failed
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Supplier yata = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        productRepository.save(new Product("ROG CROSSHAIR VIII DARK HERO", "empty", 300.15, 319.50, 0, 5, 30, asus, motherboard, yata));
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/stock-outs")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isPreconditionFailed());

    }

    @Test
    public void serviceLevel() throws Exception {
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Brand intel = brandRepository.save(new Brand("Intel"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));

        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, supplier));
        Product i9 = productRepository.save(new Product("Core i9-12900K", "empty", 430.40, 445.0, 20, 2, 10, intel, processor, supplier));

        Supplier samsung = supplierRepository.save(new Supplier("Samsung", "supplier@samsung.com", "", null));
        SupplierOrder supplierOrder1 = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(2), false, true, samsung));

        Supplier jaloo = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        SupplierOrder supplierOrder2 = supplierOrderRepository.save(new SupplierOrder(LocalDate.now().plusDays(31), LocalDate.now().plusDays(40), true, true, jaloo));

        supplierOrderLineRepository.save(new SupplierOrderLine(6, 290.0, supplierOrder1, z690f));
        supplierOrderLineRepository.save(new SupplierOrderLine(2, 310.0, supplierOrder2, i9));

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(2, 340.0, customerOrder, z690f));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/service-level")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("resultName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("resultName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("resultName").value("Service-level"))
                .andExpect(MockMvcResultMatchers.jsonPath("resultValue").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("resultValue").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("resultValue").value(1/2))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty());
    }

    @Test
    public void serviceLevel_NoContent() throws Exception {
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Customer customer = customerRepository.save(new Customer("Lara", "Clette", "laraclette@gmail.com", null));

        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, supplier));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/service-level")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, customer));
        customerOrderLineRepository.save(new CustomerOrderLine(2, 340.0, customerOrder, z690f));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/statistics/service-level")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}