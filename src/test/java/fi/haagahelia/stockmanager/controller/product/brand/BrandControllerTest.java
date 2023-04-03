package fi.haagahelia.stockmanager.controller.product.brand;

import com.google.gson.Gson;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
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

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BrandControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SupplierRepository supplierRepository;

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
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").accept(MediaType.APPLICATION_JSON).content(requestBody).header("Content-Type", MediaType.APPLICATION_JSON)).andReturn();
        if (mvcResult.getResponse().getStatus() == 200) {
            AuthResponseDTO authResponseDTO = new Gson().fromJson(mvcResult.getResponse().getContentAsString(), AuthResponseDTO.class);
            token = "Bearer " + authResponseDTO.getToken();
        } else {
            throw new RuntimeException("AUTHENTICATION FAILED!!");
        }
    }

    @Test
    public void getAllBrands() throws Exception {
        brandRepository.save(new Brand("Cailler"));
        brandRepository.save(new Brand("Sprüngli"));
        brandRepository.save(new Brand("Lindt"));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/brands")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.brandDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.brandDTOList[*].id").isNotEmpty());
    }

    @Test
    public void getAllBrands_NoBrands() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/brands")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().isNoContent())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.brandDTOList").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.brandDTOList[*].id").doesNotExist());
    }

    @Test
    public void getBrandById() throws Exception {
        Brand cailler = brandRepository.save(new Brand("Cailler"));
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/brands/" + cailler.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.brands.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.brands.href").isNotEmpty());
    }

    @Test
    public void getBrandById_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/brands/" + 999L)
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createNewBrand() throws Exception {
        Brand brand = new Brand("Lindt");
        String requestBody = new Gson().toJson(brand);

        mvc.perform(MockMvcRequestBuilders.post("/api/brands").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.brands.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.brands.href").isNotEmpty());
    }

    @Test
    public void createNewBrand_NullName() throws Exception {
        Brand brand = new Brand(null);
        String requestBody = new Gson().toJson(brand);

        mvc.perform(MockMvcRequestBuilders.post("/api/brands").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createNewBrand_EmptyName() throws Exception {
        Brand brand = new Brand("");
        String requestBody = new Gson().toJson(brand);

        mvc.perform(MockMvcRequestBuilders.post("/api/brands").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createNewBrand_AlreadyExists() throws Exception {
        Brand brand = new Brand("Lindt");
        String requestBody = new Gson().toJson(brand);

        mvc.perform(MockMvcRequestBuilders.post("/api/brands").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated());

        mvc.perform(MockMvcRequestBuilders.post("/api/brands").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void deleteBrand() throws Exception {
        Brand brand = brandRepository.save(new Brand("Sprüngli"));

        mvc.perform(MockMvcRequestBuilders.delete("/api/brands/" + brand.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteBrand_WrongId() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/brands/" + 999L).accept(MediaType.APPLICATION_JSON)
                    .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteBrand_Conflict() throws Exception {
        Supplier supplier = supplierRepository.save(new Supplier("Tester", "tester@gmail.com", null, null));
        Category category = categoryRepository.save(new Category("Chocolate", "For chocolate products"));
        Brand brand = brandRepository.save(new Brand("Sprüngli"));
        productRepository.save(new Product("Toblerone", "Matterhorn", 10.40, 13.50, 20, 5, 100, brand, category, supplier));

        mvc.perform(MockMvcRequestBuilders.delete("/api/brands/" + brand.getId()).accept(MediaType.APPLICATION_JSON)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }
}