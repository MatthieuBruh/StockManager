package fi.haagahelia.stockmanager.controller.supplier;

import com.google.gson.Gson;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.supplier.SupplierCuDTO;
import fi.haagahelia.stockmanager.model.common.Geolocation;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.product.brand.Brand;
import fi.haagahelia.stockmanager.model.product.category.Category;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.common.GeolocationRepository;
import fi.haagahelia.stockmanager.repository.product.BrandRepository;
import fi.haagahelia.stockmanager.repository.product.CategoryRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import fi.haagahelia.stockmanager.repository.supplier.SupplierRepository;
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
public class SupplierControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private GeolocationRepository geoRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierOrderRepository supplierOrderRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

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
    public void getSuppliers() throws Exception {
        supplierRepository.save(new Supplier("Samsung", "supplier@samsung.com", "", null));
        supplierRepository.save(new Supplier("Einti", "supplier@einti.com", "843247447", null));
        supplierRepository.save(new Supplier("Jaloo", "supplier@jaloo.com", "", null));
        supplierRepository.save(new Supplier("Eare", "supplier@eare.com", "32343422", null));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.supplierDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.supplierDTOList[*].id").isNotEmpty());
    }

    @Test
    public void getSupplierByID() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Ratapihantie", "13", "00250", "Helsinki", "Finland"));
        Supplier supplier = supplierRepository.save(new Supplier("Einti", "supplier@einti.com", "843247447", geo));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/" + supplier.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("name").value(supplier.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("email").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(supplier.getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("phoneNumber").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("phoneNumber").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("phoneNumber").value(supplier.getPhoneNumber()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.suppliers.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.suppliers.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.geolocation.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.geolocation.href").isNotEmpty());
    }

    @Test
    public void getSupplierByID_WrongId() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/suppliers/" + 999L)
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createNewSupplier() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Ratapihantie", "13", "00250", "Helsinki", "Finland"));
        SupplierCuDTO supplierCuDTO = new SupplierCuDTO(); supplierCuDTO.setEmail("test@supplier.com");
        supplierCuDTO.setName("Supplier name"); supplierCuDTO.setPhoneNumber("0219392"); supplierCuDTO.setGeolocationId(geo.getId());
        String requestBody = new Gson().toJson(supplierCuDTO);

        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("name").value(supplierCuDTO.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("email").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(supplierCuDTO.getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("phoneNumber").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("phoneNumber").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("phoneNumber").value(supplierCuDTO.getPhoneNumber()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.suppliers.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.suppliers.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.geolocation.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.geolocation.href").isNotEmpty());
    }

    @Test
    public void createNewSupplier_WrongValues() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Ratapihantie", "13", "00250", "Helsinki", "Finland"));
        SupplierCuDTO supplierCuDTO = new SupplierCuDTO(); supplierCuDTO.setEmail("test@supplier.com");
        supplierCuDTO.setName("Supplier name"); supplierCuDTO.setPhoneNumber("0219392"); supplierCuDTO.setGeolocationId(geo.getId());
        String requestBody;

        // Null name
        supplierCuDTO.setName(null);
        requestBody = new Gson().toJson(supplierCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        supplierCuDTO.setName("Supplier name");

        // Empty name
        supplierCuDTO.setName("");
        requestBody = new Gson().toJson(supplierCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        supplierCuDTO.setName("Supplier name");

        // Wrong geo Id
        supplierCuDTO.setGeolocationId(99L);
        requestBody = new Gson().toJson(supplierCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                        .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        supplierCuDTO.setGeolocationId(geo.getId());

        // Already exists
        requestBody = new Gson().toJson(supplierCuDTO);
        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isCreated());

        mvc.perform(MockMvcRequestBuilders.post("/api/suppliers").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void updateSupplier() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Ratapihantie", "13", "00250", "Helsinki", "Finland"));
        Supplier supplier = supplierRepository.save(new Supplier("Zooko", "zooko@gmail.com", "", geo));

        SupplierCuDTO supplierCuDTO = new SupplierCuDTO(); supplierCuDTO.setName(supplier.getName());
        supplierCuDTO.setEmail("supplier@zooko.com"); supplierCuDTO.setPhoneNumber("3239932"); supplierCuDTO.setGeolocationId(geo.getId());
        String requestBody = new Gson().toJson(supplierCuDTO);

        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/" + supplier.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("name").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("name").value(supplierCuDTO.getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("email").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("email").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("email").value(supplierCuDTO.getEmail()))
                .andExpect(MockMvcResultMatchers.jsonPath("phoneNumber").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("phoneNumber").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("phoneNumber").value(supplierCuDTO.getPhoneNumber()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.suppliers.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.suppliers.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.geolocation.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.geolocation.href").isNotEmpty());
    }

    @Test
    public void updateSupplier_WrongValues() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Ratapihantie", "13", "00250", "Helsinki", "Finland"));
        Supplier supplier = supplierRepository.save(new Supplier("Zooko", "zooko@gmail.com", "", geo));
        SupplierCuDTO supplierCuDTO = new SupplierCuDTO(); supplierCuDTO.setName(supplier.getName());
        supplierCuDTO.setEmail("supplier@zooko.com"); supplierCuDTO.setPhoneNumber("3239932"); supplierCuDTO.setGeolocationId(geo.getId());
        String requestBody;

        // Null name
        supplierCuDTO.setName(null);
        requestBody = new Gson().toJson(supplierCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/" + supplier.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        supplierCuDTO.setName(supplier.getName());

        // Empty name
        supplierCuDTO.setName("");
        requestBody = new Gson().toJson(supplierCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/" + supplier.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        supplierCuDTO.setName(supplier.getName());

        // Wrong name
        supplierCuDTO.setName("WRONG NAME");
        requestBody = new Gson().toJson(supplierCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/" + supplier.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
        supplierCuDTO.setName(supplier.getName());

        // Wrong geolocation id
        supplierCuDTO.setGeolocationId(999L);
        requestBody = new Gson().toJson(supplierCuDTO);
        mvc.perform(MockMvcRequestBuilders.put("/api/suppliers/" + supplier.getId()).accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isNotFound());
        supplierCuDTO.setGeolocationId(geo.getId());
    }

    @Test
    public void deleteSupplierById() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Ratapihantie", "13", "00250", "Helsinki", "Finland"));
        Supplier supplier = supplierRepository.save(new Supplier("Zooko", "zooko@gmail.com", "", geo));

        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/" + supplier.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteSupplierById_WrongID() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/" + 999L).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteSupplierById_ProductRelated() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Ratapihantie", "13", "00250", "Helsinki", "Finland"));
        Supplier supplier = supplierRepository.save(new Supplier("Zooko", "zooko@gmail.com", "", geo));
        Category category = categoryRepository.save(new Category("Chocolate", "For chocolate products"));
        Brand brand = brandRepository.save(new Brand("Sprüngli"));
        productRepository.save(new Product("Toblerone", "Matterhorn", 10.40, 13.50, 20, 5, 100, brand, category, supplier));

        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/" + supplier.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    public void deleteSupplierById_SupplierOrder() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Ratapihantie", "13", "00250", "Helsinki", "Finland"));
        Supplier supplier = supplierRepository.save(new Supplier("Zooko", "zooko@gmail.com", "", geo));
        supplierOrderRepository.save(new SupplierOrder(LocalDate.now(), LocalDate.now().plusDays(7), false, false, supplier));

        mvc.perform(MockMvcRequestBuilders.delete("/api/suppliers/" + supplier.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token))
                .andExpect(status().isConflict());
    }
}