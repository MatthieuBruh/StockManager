package fi.haagahelia.stockmanager.controller.supplier.order;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.supplier.order.SupplierOrderCuDTO;
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
import fi.haagahelia.stockmanager.tools.LocalDateAdapter;
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
public class SupplierOrderControllerTest {

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
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private CustomerOrderLineRepository customerOrderLineRepository;

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
    public void getSupplierOrders() throws Exception {
        Supplier samsung = supplierRepository.save(new Supplier("Samsung", "supplier@samsung.com", "", null));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, samsung));
        Supplier einti = supplierRepository.save(new Supplier("Einti", "supplier@einti.com", "843247447", null));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(2), false, false, einti));
        Supplier jaloo = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(10), false, false, jaloo));
        Supplier eare = supplierRepository.save(new Supplier("Eare", "supplier@eare.com", "32343422", null));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(26), false, false, eare));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.supplierOrderDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.supplierOrderDTOList[*].id").isNotEmpty());
    }

    @Test
    public void getSupplierOrders_NoContent() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.supplierOrderDTOList").doesNotExist());
    }

    @Test
    public void getSupplierOrder() throws Exception {
        Supplier supplier = supplierRepository.save(new Supplier("Samsung", "supplier@samsung.com", "", null));
        SupplierOrder order = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, supplier));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders/" + supplier.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(order.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(order.getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").value(order.getDeliveryDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").value(order.getOrderIsSent()))
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").value(order.getReceived()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier-orders.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-supplier-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-supplier-orders.href").isNotEmpty());
    }

    @Test
    public void getSupplierOrder_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders/" + 999L)
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getSpecSupplierOrders() throws Exception {
        Supplier supplier = supplierRepository.save(new Supplier("Samsung", "supplier@samsung.com", "", null));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, supplier));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(8), false, false, supplier));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/" + supplier.getId() + "/orders")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.supplierOrderDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.supplierOrderDTOList[*].id").isNotEmpty());
    }

    @Test
    public void getSpecSupplierOrders_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/" + 999L + "/orders")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getSpecSupplierOrders_NoContent() throws Exception {
        Supplier supplier = supplierRepository.save(new Supplier("Samsung", "supplier@samsung.com", "", null));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/" + supplier.getId() + "/orders")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.supplierOrderDTOList").doesNotExist());
    }

    @Test
    public void getSupOrdersDate() throws Exception {
        Supplier samsung = supplierRepository.save(new Supplier("Samsung", "supplier@samsung.com", "", null));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, samsung));
        Supplier einti = supplierRepository.save(new Supplier("Einti", "supplier@einti.com", "843247447", null));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(12), false, false, einti));
        Supplier jaloo = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(10), false, false, jaloo));
        Supplier eare = supplierRepository.save(new Supplier("Eare", "supplier@eare.com", "32343422", null));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(12), false, false, eare));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders/delivery=" + LocalDate.now().plusDays(12))
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.supplierOrderDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.supplierOrderDTOList[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.supplierOrderDTOList").isArray());
    }

    @Test
    public void getSupOrdersDate_NoContent() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders/delivery=" + LocalDate.now().plusDays(30))
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void createSupplierOrder() throws Exception {
        Supplier jaloo = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        SupplierOrderCuDTO supplierOrderCuDTO = new SupplierOrderCuDTO(LocalDate.now(), LocalDate.now().plusDays(3), false, false, jaloo.getId());
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        String requestBody = gson.toJson(supplierOrderCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers/orders").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(supplierOrderCuDTO.getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").value(supplierOrderCuDTO.getDeliveryDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").value(supplierOrderCuDTO.getOrderIsSent()))
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").value(supplierOrderCuDTO.getIsReceived()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier-orders.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-supplier-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-supplier-orders.href").isNotEmpty());
    }

    @Test
    public void createSupplierOrder_WrongSupplierId() throws Exception {
        SupplierOrderCuDTO supplierOrderCuDTO = new SupplierOrderCuDTO(LocalDate.now(), LocalDate.now().plusDays(3), false, false, 999L);
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        String requestBody = gson.toJson(supplierOrderCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers/orders").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateSupOrder() throws Exception {
        Supplier supplier = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, supplier));
        SupplierOrderCuDTO supplierOrderCuDTO = new SupplierOrderCuDTO(LocalDate.now(), LocalDate.now().plusDays(3), false, false, supplier.getId());
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        String requestBody = gson.toJson(supplierOrderCuDTO);

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(supplierOrderCuDTO.getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").value(supplierOrderCuDTO.getDeliveryDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").value(supplierOrderCuDTO.getOrderIsSent()))
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").value(supplierOrderCuDTO.getIsReceived()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier-orders.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-supplier-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-supplier-orders.href").isNotEmpty());
    }

    @Test
    public void updateSupOrder_BadRequest() throws Exception {
        Supplier supplier = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        SupplierOrderCuDTO supplierOrderCuDTO = new SupplierOrderCuDTO(LocalDate.now(), LocalDate.now().plusDays(3), false, false, supplier.getId());
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        String requestBody = gson.toJson(supplierOrderCuDTO);

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + 77L).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void sendOrder() throws Exception {
        Category category = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand brand = brandRepository.save(new Brand("AMD"));
        Supplier supplier = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, supplier));
        supplierOrderLineRepository.save(new SupplierOrderLine(5, 350.30, supplierOrder, product));

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(supplierOrder.getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").value(supplierOrder.getDeliveryDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").value(supplierOrder.getReceived()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier-orders.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-supplier-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-supplier-orders.href").isNotEmpty());
    }

    @Test
    public void sendOrder_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + 77L + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void sendOrder_Conflict() throws Exception {
        Supplier supplier = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), true, false, supplier));

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void receivedOrder() throws Exception {
        Category category = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand brand = brandRepository.save(new Brand("AMD"));
        Supplier supplier = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, supplier));
        supplierOrderLineRepository.save(new SupplierOrderLine(5, 350.30, supplierOrder, product));

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/received").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(supplierOrder.getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").value(supplierOrder.getDeliveryDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier-orders.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-supplier-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-supplier-orders.href").isNotEmpty());
    }

    @Test
    public void receivedOrder_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + 999L + "/received").accept(MediaType.APPLICATION_JSON)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void receivedOrder_Conflict() throws Exception {
        Category category = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand brand = brandRepository.save(new Brand("AMD"));
        Supplier supplier = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, supplier));
        supplierOrderLineRepository.save(new SupplierOrderLine(5, 350.30, supplierOrder, product));

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/received").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/received").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void cancelReceivedOrder() throws Exception {
        Category category = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand brand = brandRepository.save(new Brand("AMD"));
        Supplier supplier = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, supplier));
        supplierOrderLineRepository.save(new SupplierOrderLine(5, 350.30, supplierOrder, product));

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/received").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/cancel-reception").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("date").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("date").value(supplierOrder.getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("deliveryDate").value(supplierOrder.getDeliveryDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("orderIsSent").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("isReceived").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier-orders.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-supplier-orders.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.this-supplier-orders.href").isNotEmpty());
    }

    @Test
    public void cancelReceivedOrder_NotReceived() throws Exception {
        Category category = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand brand = brandRepository.save(new Brand("AMD"));
        Supplier supplier = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, supplier));
        supplierOrderLineRepository.save(new SupplierOrderLine(5, 350.30, supplierOrder, product));

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/cancel-reception").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void cancelReceivedOrder_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + 999L + "/received").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void cancelReceivedOrder_ProductStockError() throws Exception {
        Category category = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand brand = brandRepository.save(new Brand("AMD"));
        Supplier supplier = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, supplier));
        supplierOrderLineRepository.save(new SupplierOrderLine(5, 350.30, supplierOrder, product));

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/received").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());

        // Simulate that a part of the stock has been sold.
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, null));
        customerOrderLineRepository.save(new CustomerOrderLine(33, 340.0, customerOrder, product));

        mvc.perform(MockMvcRequestBuilders.put("/api/customers/orders/" + customerOrder.getId() + "/send").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());

        /*
        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/orders/" + supplierOrder.getId() + "/cancel-reception").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotModified());
        */
    }

    @Test
    public void deleteSupOrder() throws Exception {
        Supplier supplier = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), false, false, supplier));

        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/orders/" + supplierOrder.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteSupOrder_Wrong() throws Exception {
        Supplier supplier = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now().minusDays(4), LocalDate.now().plusDays(3), false, false, supplier));
        SupplierOrder supplierOrder2 = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(3), true, false, supplier));

        // Order is too old
        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/orders/" + supplierOrder.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isPreconditionFailed());

        // Order is sent
        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/orders/" + supplierOrder2.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isPreconditionFailed());

        // Wrong id
        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/orders/" + 989L).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteOrderForce() throws Exception {
        Supplier supplier = supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now().minusDays(4), LocalDate.now().plusDays(3), false, false, supplier));

        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/orders/" + supplierOrder.getId() + "/force").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteOrderForce_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/orders/" + 999L + "/force").accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }
}