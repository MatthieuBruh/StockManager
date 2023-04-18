package fi.haagahelia.stockmanager.controller.supplier.order;

import com.google.gson.Gson;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.supplier.order.SupplierOrderLineCuDTO;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLine;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class SupplierOrderLineControllerTest {

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
    public void getSupOrderLines() throws Exception {
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Brand intel = brandRepository.save(new Brand("Intel"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));

        Product z690f = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, supplier));
        Product i9 = productRepository.save(new Product("Core i9-12900K", "empty", 430.40, 445.0, 20, 2, 10, intel, processor, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier));
        supplierOrderLineRepository.save(new SupplierOrderLine(3, 340.0, supplierOrder, z690f));
        supplierOrderLineRepository.save(new SupplierOrderLine(3, 340.0, supplierOrder, i9));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.supplierOrderLineDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty());
    }

    @Test
    public void getSupOrderLines_NoContent() throws Exception {
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void getSupOrderLines_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders/order=" + 999L + "/details")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getSupOrderLine() throws Exception {
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier));
        supplierOrderLineRepository.save(new SupplierOrderLine(3, 340.0, supplierOrder, product));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details/product=" + product.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("quantity").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("quantity").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("buyPrice").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("buyPrice").isNotEmpty())
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
    public void getSupOrderLine_BadRequest() throws Exception {
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier));
        supplierOrderLineRepository.save(new SupplierOrderLine(3, 340.0, supplierOrder, product));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders/order=" + 865L + "/details/product=" + product.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isBadRequest());

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details/product=" + 999L)
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getSupOrderLine_NoContent() throws Exception {
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details/product=" + product.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    public void createOrderLine() throws Exception {
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 20, 10, 20, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier));
        SupplierOrderLineCuDTO supplierOrderLineCuDTO = new SupplierOrderLineCuDTO(10, 301.10);
        String requestBody = new Gson().toJson(supplierOrderLineCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("quantity").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("quantity").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("quantity").value(supplierOrderLineCuDTO.getQuantity()))
                .andExpect(MockMvcResultMatchers.jsonPath("buyPrice").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("buyPrice").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("buyPrice").value(supplierOrderLineCuDTO.getBuyPrice()))
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
    public void createOrderLine_OrderIssues() throws Exception {
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 20, 10, 20, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now().minusDays(10), LocalDate.now().minusDays(7), true, false, supplier));
        SupplierOrderLineCuDTO supplierOrderLineCuDTO = new SupplierOrderLineCuDTO(10, 301.10);
        String requestBody = new Gson().toJson(supplierOrderLineCuDTO);

        // Order not found
        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers/orders/order=" + 999L + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());

        // Order too old
        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    public void createOrderLine_ProductIssues() throws Exception {
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 20, 10, 20, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier));
        SupplierOrderLineCuDTO supplierOrderLineCuDTO = new SupplierOrderLineCuDTO(10, 301.10);
        String requestBody = new Gson().toJson(supplierOrderLineCuDTO);

        // Product not found
        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details/product=" + 999L).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createOrderLine_OtherIssues() throws Exception {
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Supplier supplier2 = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 20, 10, 20, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier));
        SupplierOrder supplierOrder2 = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier2));
        SupplierOrderLineCuDTO supplierOrderLineCuDTO = new SupplierOrderLineCuDTO(10, 301.10);
        String requestBody = new Gson().toJson(supplierOrderLineCuDTO);

        // Wrong supplier
        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers/orders/order=" + supplierOrder2.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isPreconditionFailed());

        // Wrong quantity
        supplierOrderLineCuDTO.setQuantity(-10);
        requestBody = new Gson().toJson(supplierOrderLineCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isPreconditionFailed());
        supplierOrderLineCuDTO.setQuantity(3);

        // Order line already exists
        requestBody = new Gson().toJson(supplierOrderLineCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isCreated());

        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    public void deleteOrderLine() throws Exception {
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier));
        supplierOrderLineRepository.save(new SupplierOrderLine(3, 340.0, supplierOrder, product));

        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteOrderLine_Wrong() throws Exception {
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Brand brand = brandRepository.save(new Brand("Asus"));
        Supplier supplier = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Product product = productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, brand, category, supplier));

        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), true, false, supplier));
        supplierOrderLineRepository.save(new SupplierOrderLine(3, 340.0, supplierOrder, product));

        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/orders/order=" + 999L + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());

        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details/product=" + 888L).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());

        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/orders/order=" + supplierOrder.getId() + "/details/product=" + product.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isPreconditionFailed());
    }
}