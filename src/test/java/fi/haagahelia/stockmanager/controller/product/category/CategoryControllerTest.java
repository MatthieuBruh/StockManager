package fi.haagahelia.stockmanager.controller.product.category;

import com.google.gson.Gson;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.product.category.CategoryCuDTO;
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
import org.junit.jupiter.api.TestInstance;
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

import java.io.UnsupportedEncodingException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CategoryControllerTest {

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
    public void getALlCategories() throws Exception {
        categoryRepository.save(new Category("Motherboard", "For the motherboards"));
        categoryRepository.save(new Category("Processor", "For the processors"));
        categoryRepository.save(new Category("Graphic Cards", "For the graphic cards"));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/categories")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.categoryDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.categoryDTOList[*].id").isNotEmpty());
    }

    @Test
    public void getALlCategories_NoCategoryFound() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/categories")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    public void getCategoryndById() throws Exception {
        Category category = categoryRepository.save(new Category("Graphic Cards", "For the graphic cards"));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/categories/" + category.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("description").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("description").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.categories.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.categories.href").isNotEmpty());
    }

    @Test
    public void getCategoryndById_WrongId() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/categories/" + 9999L)
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createCategory() throws Exception {
        CategoryCuDTO categoryCuDTO = new CategoryCuDTO(); categoryCuDTO.setName("Processor"); categoryCuDTO.setDescription("For the processors");
        String requestBody = new Gson().toJson(categoryCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/categories").accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("description").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("description").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.categories.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.categories.href").isNotEmpty());
    }

    @Test
    public void createCategory_NullName() throws Exception {
        CategoryCuDTO categoryCuDTO = new CategoryCuDTO(); categoryCuDTO.setName(null); categoryCuDTO.setDescription("For the processors");
        String requestBody = new Gson().toJson(categoryCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/categories").accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createCategory_EmptyName() throws Exception {
        CategoryCuDTO categoryCuDTO = new CategoryCuDTO(); categoryCuDTO.setName(""); categoryCuDTO.setDescription("For the processors");
        String requestBody = new Gson().toJson(categoryCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/categories").accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createCategory_AlreadyExists() throws Exception {
        CategoryCuDTO categoryCuDTO = new CategoryCuDTO(); categoryCuDTO.setName("CPU"); categoryCuDTO.setDescription("For the processors");
        String requestBody = new Gson().toJson(categoryCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/categories").accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated());

        mvc.perform(MockMvcRequestBuilders.post("/api/categories").accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void updateCategory() throws Exception {
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));

        CategoryCuDTO categoryCuDTO = new CategoryCuDTO(); categoryCuDTO.setName("CPU"); categoryCuDTO.setDescription("For the motherboards..");
        String requestBody = new Gson().toJson(categoryCuDTO);

        mvc.perform(MockMvcRequestBuilders.put("/api/categories/" + category.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("description").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("description").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("description").value(categoryCuDTO.getDescription()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.categories.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.categories.href").isNotEmpty());
    }

    @Test
    public void updateCategory_WrongID() throws Exception {
        CategoryCuDTO categoryCuDTO = new CategoryCuDTO(); categoryCuDTO.setName("CPU"); categoryCuDTO.setDescription("For the motherboards..");
        String requestBody = new Gson().toJson(categoryCuDTO);

        mvc.perform(MockMvcRequestBuilders.put("/api/categories/" + 999L).accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteCategory() throws Exception {
        Category category = categoryRepository.save(new Category("Motherboard", "For the motherboards"));

        mvc.perform(MockMvcRequestBuilders.delete("/api/categories/" + category.getId()).accept(MediaType.APPLICATION_JSON)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteCategory_BadRequest() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/categories/" + 999L).accept(MediaType.APPLICATION_JSON)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteCategory_Conflict() throws Exception {
        Supplier supplier = supplierRepository.save(new Supplier("Tester", "tester@gmail.com", null, null));
        Category category = categoryRepository.save(new Category("Chocolate", "For chocolate products"));
        Brand brand = brandRepository.save(new Brand("Sprüngli"));
        productRepository.save(new Product("Toblerone", "Matterhorn", 10.40, 13.50, 20, 5, 100, brand, category, supplier));

        mvc.perform(MockMvcRequestBuilders.delete("/api/categories/" + category.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }
}