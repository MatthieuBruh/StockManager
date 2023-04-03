package fi.haagahelia.stockmanager.controller.product;

import com.google.gson.Gson;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.product.ProductCuDTO;
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
public class ProductControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierOrderRepository supplierOrderRepository;

    @Autowired
    private SupplierOrderLineRepository supplierOrderLineRepository;

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
    public void getProduct() throws Exception {
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Category graphicCard = categoryRepository.save(new Category("Graphic Cards", "For the graphic cards"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Brand intel = brandRepository.save(new Brand("Intel"));
        Brand amd = brandRepository.save(new Brand("AMD"));
        Supplier yata = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Supplier midel = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));

        productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 20, 10, 20, asus, motherboard, yata));
        productRepository.save(new Product("ROG CROSSHAIR VIII DARK HERO", "empty", 300.15, 319.50, 20, 5, 30, asus, motherboard, yata));
        productRepository.save(new Product("Core i9-12900K", "empty", 430.40, 445.0, 20, 2, 10, intel, processor, yata));
        productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, amd, processor, midel));
        productRepository.save(new Product("Graphique Arc A750", "empty", 250.75, 301.10, 20, 4, 20, intel, graphicCard, midel));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/products")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.productSimpleDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.productSimpleDTOList[*].id").isNotEmpty());
    }

    @Test
    public void getProduct_NoContent() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/products")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testGetProduct() throws Exception {
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand amd = brandRepository.save(new Brand("AMD"));
        Supplier midel = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, amd, processor, midel));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/products/" + product.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(product.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("name").value(product.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("description").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("description").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("description").value(product.getDescription()))
                .andExpect(MockMvcResultMatchers.jsonPath("salePrice").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("salePrice").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("salePrice").value(product.getSalePrice()))
                .andExpect(MockMvcResultMatchers.jsonPath("stock").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("stock").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("stock").value(product.getStock()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.products.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.products.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.category.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.category.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.brand.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.brand.href").isNotEmpty());
    }

    @Test
    public void testGetProduct_WrongId() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/products/" + 999L)
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getProdDetail() throws Exception {
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand amd = brandRepository.save(new Brand("AMD"));
        Supplier midel = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, amd, processor, midel));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/products/" + product.getId() + "/details")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(product.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("name").value(product.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("description").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("description").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("description").value(product.getDescription()))
                .andExpect(MockMvcResultMatchers.jsonPath("purchasePrice").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("purchasePrice").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("purchasePrice").value(product.getPurchasePrice()))
                .andExpect(MockMvcResultMatchers.jsonPath("salePrice").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("salePrice").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("salePrice").value(product.getSalePrice()))
                .andExpect(MockMvcResultMatchers.jsonPath("stock").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("stock").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("stock").value(product.getStock()))
                .andExpect(MockMvcResultMatchers.jsonPath("minStock").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("minStock").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("minStock").value(product.getMinStock()))
                .andExpect(MockMvcResultMatchers.jsonPath("batchSize").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("batchSize").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("batchSize").value(product.getBatchSize()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.products.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.products.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.category.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.category.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.brand.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.brand.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").isNotEmpty());
    }

    @Test
    public void getProdDetail_WrongId() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/products/" + 999L + "/details")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getLowStockProd() throws Exception {
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Category graphicCard = categoryRepository.save(new Category("Graphic Cards", "For the graphic cards"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Brand intel = brandRepository.save(new Brand("Intel"));
        Brand amd = brandRepository.save(new Brand("AMD"));
        Supplier yata = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));
        Supplier midel = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));

        productRepository.save(new Product("ROG Strix Z690-F", "empty", 310.40, 350.50, 5, 10, 20, asus, motherboard, yata));
        productRepository.save(new Product("ROG CROSSHAIR VIII DARK HERO", "empty", 300.15, 319.50, 20, 5, 30, asus, motherboard, yata));
        productRepository.save(new Product("Core i9-12900K", "empty", 430.40, 445.0, 20, 2, 10, intel, processor, yata));
        productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 9, 10, 3, amd, processor, midel));
        productRepository.save(new Product("Graphique Arc A750", "empty", 250.75, 301.10, 7, 15, 20, intel, graphicCard, midel));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/products/low")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.productCompleteDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.productCompleteDTOList[*].id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("page.totalElements").value(3));
    }

    @Test
    public void getLowStockProd_NoContent() throws Exception {
        Category motherboard = categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand asus = brandRepository.save(new Brand("Asus"));
        Brand intel = brandRepository.save(new Brand("Intel"));
        Supplier yata = supplierRepository.save(new Supplier("Yata", "supplier@yata.com", null, null));

        productRepository.save(new Product("ROG CROSSHAIR VIII DARK HERO", "empty", 300.15, 319.50, 20, 5, 30, asus, motherboard, yata));
        productRepository.save(new Product("Core i9-12900K", "empty", 430.40, 445.0, 20, 2, 10, intel, processor, yata));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/products/low?page=10")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void createProduct() throws Exception {
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand amd = brandRepository.save(new Brand("AMD"));
        Supplier midel = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));

        ProductCuDTO productCuDTO = new ProductCuDTO();
        productCuDTO.setName("Ryzen 9 5900X"); productCuDTO.setDescription("Ultimate processor for gamers");
        productCuDTO.setPurchasePrice(300.20); productCuDTO.setSalePrice(346.20); productCuDTO.setStock(20);
        productCuDTO.setMinStock(10); productCuDTO.setBatchSize(20); productCuDTO.setBrandId(amd.getId());
        productCuDTO.setCategoryId(processor.getId()); productCuDTO.setSupplierId(midel.getId());
        String requestBody = new Gson().toJson(productCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("name").value(productCuDTO.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("description").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("description").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("description").value(productCuDTO.getDescription()))
                .andExpect(MockMvcResultMatchers.jsonPath("purchasePrice").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("purchasePrice").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("purchasePrice").value(productCuDTO.getPurchasePrice()))
                .andExpect(MockMvcResultMatchers.jsonPath("salePrice").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("salePrice").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("salePrice").value(productCuDTO.getSalePrice()))
                .andExpect(MockMvcResultMatchers.jsonPath("stock").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("stock").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("stock").value(productCuDTO.getStock()))
                .andExpect(MockMvcResultMatchers.jsonPath("minStock").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("minStock").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("minStock").value(productCuDTO.getMinStock()))
                .andExpect(MockMvcResultMatchers.jsonPath("batchSize").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("batchSize").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("batchSize").value(productCuDTO.getBatchSize()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.products.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.products.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.category.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.category.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.brand.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.brand.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").isNotEmpty());
    }

    @Test
    public void createProduct_WrongValues() throws Exception {
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand amd = brandRepository.save(new Brand("AMD"));
        Supplier midel = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));

        ProductCuDTO productCuDTO = new ProductCuDTO();
        productCuDTO.setName("Ryzen 9 5900X"); productCuDTO.setDescription("Ultimate processor for gamers");
        productCuDTO.setPurchasePrice(350.20); productCuDTO.setSalePrice(346.20); productCuDTO.setStock(20);
        productCuDTO.setMinStock(10); productCuDTO.setBatchSize(20); productCuDTO.setBrandId(amd.getId());
        productCuDTO.setCategoryId(processor.getId()); productCuDTO.setSupplierId(midel.getId());
        String requestBody;

        // Null value
        productCuDTO.setName(null);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setName("Ryzen 9 5900X");

        productCuDTO.setDescription(null);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setDescription("Ultimate processor for gamers");

        productCuDTO.setBrandId(null);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        productCuDTO.setBrandId(amd.getId());

        productCuDTO.setCategoryId(null);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        productCuDTO.setCategoryId(processor.getId());

        productCuDTO.setSupplierId(null);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        productCuDTO.setSupplierId(midel.getId());

        // =============================================================================================================

        // Empty Values or too low values
        productCuDTO.setName("");
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setName("Ryzen 9 5900X");

        productCuDTO.setDescription("");
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setDescription("\"Ultimate processor for gamers\"");

        productCuDTO.setPurchasePrice(-10.0);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setPurchasePrice(300.20);

        productCuDTO.setSalePrice(-10.0);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setSalePrice(346.20);

        productCuDTO.setStock(-5);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setStock(20);

        productCuDTO.setMinStock(-2);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setMinStock(10);

        productCuDTO.setBatchSize(-30);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setBatchSize(20);

        // =============================================================================================================

        // Wrong id

        productCuDTO.setBrandId(99L);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        productCuDTO.setBrandId(amd.getId());

        productCuDTO.setCategoryId(999L);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        productCuDTO.setCategoryId(processor.getId());

        productCuDTO.setSupplierId(999L);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        productCuDTO.setSupplierId(midel.getId());

        // =============================================================================================================

        // Already exists
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated());

        mvc.perform(MockMvcRequestBuilders.post("/api/products").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void updateProduct() throws Exception {
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand amd = brandRepository.save(new Brand("AMD"));
        Supplier midel = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, amd, processor, midel));

        ProductCuDTO productCuDTO = new ProductCuDTO(); productCuDTO.setId(product.getId());
        productCuDTO.setName(product.getName()); productCuDTO.setDescription("UPDATED DESCRIPTION");
        productCuDTO.setPurchasePrice(325.30); productCuDTO.setSalePrice(340.50);
        productCuDTO.setStock(product.getStock());productCuDTO.setMinStock(15);
        productCuDTO.setBatchSize(8); productCuDTO.setBrandId(product.getBrand().getId());
        productCuDTO.setCategoryId(product.getCategory().getId()); productCuDTO.setSupplierId(product.getSupplier().getId());
        String requestBody = new Gson().toJson(productCuDTO);

        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("name").value(productCuDTO.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("description").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("description").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("description").value(productCuDTO.getDescription()))
                .andExpect(MockMvcResultMatchers.jsonPath("purchasePrice").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("purchasePrice").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("purchasePrice").value(productCuDTO.getPurchasePrice()))
                .andExpect(MockMvcResultMatchers.jsonPath("salePrice").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("salePrice").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("salePrice").value(productCuDTO.getSalePrice()))
                .andExpect(MockMvcResultMatchers.jsonPath("stock").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("stock").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("stock").value(productCuDTO.getStock()))
                .andExpect(MockMvcResultMatchers.jsonPath("minStock").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("minStock").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("minStock").value(productCuDTO.getMinStock()))
                .andExpect(MockMvcResultMatchers.jsonPath("batchSize").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("batchSize").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("batchSize").value(productCuDTO.getBatchSize()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.products.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.products.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.category.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.category.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.brand.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.brand.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.supplier.href").isNotEmpty());
    }

    @Test
    public void updateProduct_WrongValues() throws Exception {
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand amd = brandRepository.save(new Brand("AMD"));
        Supplier midel = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, amd, processor, midel));

        ProductCuDTO productCuDTO = new ProductCuDTO(); productCuDTO.setId(product.getId());
        productCuDTO.setName(product.getName()); productCuDTO.setDescription("UPDATED DESCRIPTION");
        productCuDTO.setPurchasePrice(325.30); productCuDTO.setSalePrice(340.50);
        productCuDTO.setStock(product.getStock());productCuDTO.setMinStock(15);
        productCuDTO.setBatchSize(8); productCuDTO.setBrandId(product.getBrand().getId());
        productCuDTO.setCategoryId(product.getCategory().getId()); productCuDTO.setSupplierId(product.getSupplier().getId());
        String requestBody;

        // Null value
        productCuDTO.setName(null);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setName("Ryzen 9 5900X");

        productCuDTO.setDescription(null);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setDescription("Ultimate processor for gamers");

        productCuDTO.setBrandId(null);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        productCuDTO.setBrandId(amd.getId());

        productCuDTO.setCategoryId(null);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        productCuDTO.setCategoryId(processor.getId());

        productCuDTO.setSupplierId(null);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        productCuDTO.setSupplierId(midel.getId());

        // =============================================================================================================

        // Empty Values or too low values
        productCuDTO.setName("");
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setName("Ryzen 9 5900X");

        productCuDTO.setDescription("");
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setDescription("\"Ultimate processor for gamers\"");

        productCuDTO.setPurchasePrice(-10.0);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setPurchasePrice(300.20);

        productCuDTO.setSalePrice(-10.0);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setSalePrice(346.20);

        productCuDTO.setStock(-5);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setStock(20);

        productCuDTO.setMinStock(-2);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setMinStock(10);

        productCuDTO.setBatchSize(-30);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        productCuDTO.setBatchSize(20);

        // =============================================================================================================

        // Wrong id

        productCuDTO.setBrandId(99L);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        productCuDTO.setBrandId(amd.getId());

        productCuDTO.setCategoryId(999L);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        productCuDTO.setCategoryId(processor.getId());

        productCuDTO.setSupplierId(999L);
        requestBody = new Gson().toJson(productCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        productCuDTO.setSupplierId(midel.getId());
    }

    @Test
    public void deleteProduct() throws Exception {
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand amd = brandRepository.save(new Brand("AMD"));
        Supplier midel = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, amd, processor, midel));

        mvc.perform(MockMvcRequestBuilders.delete("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteProduct_WrongId() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/products/" + 99L).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteProduct_SupplierOrderConflict() throws Exception {
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand amd = brandRepository.save(new Brand("AMD"));
        Supplier midel = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, amd, processor, midel));
        SupplierOrder supplierOrder = supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, midel));
        supplierOrderLineRepository.save(new SupplierOrderLine(3, 340.0, supplierOrder, product));

        mvc.perform(MockMvcRequestBuilders.delete("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void deleteProduct_CustomerOrderConflict() throws Exception {
        Category processor = categoryRepository.save(new Category("Processor", "For the processors"));
        Brand amd = brandRepository.save(new Brand("AMD"));
        Supplier midel = supplierRepository.save(new Supplier("Midel", "supplier@midel.com", null, null));
        Product product = productRepository.save(new Product("Ryzen 9 5900X", "empty", 340.0, 346.50, 20, 10, 3, amd, processor, midel));
        CustomerOrder customerOrder = customerOrderRepository.save(new CustomerOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, employee, null));
        customerOrderLineRepository.save(new CustomerOrderLine(3, 340.0, customerOrder, product));

        mvc.perform(MockMvcRequestBuilders.delete("/api/products/" + product.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }
}