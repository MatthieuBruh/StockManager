package fi.haagahelia.stockmanager.controller.common;

import com.google.gson.Gson;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.common.GeolocationCuDTO;
import fi.haagahelia.stockmanager.model.common.Geolocation;
import fi.haagahelia.stockmanager.model.customer.Customer;
import fi.haagahelia.stockmanager.model.supplier.Supplier;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.common.GeolocationRepository;
import fi.haagahelia.stockmanager.repository.customer.CustomerRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class GeolocationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private GeolocationRepository geoRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CustomerRepository customerRepository;

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
    public void getGeolocations() throws Exception {
        geoRepository.save(new Geolocation("Ratapihantie", "13", "00250", "Helsinki", "Finland"));
        geoRepository.save(new Geolocation("Campus Battelle, Rue de la Tambourine", "17", "1227", "Carouge", "Switzerland"));
        geoRepository.save(new Geolocation("Rte de Moutier", "14", "2800", "Delémont", "Switzerland"));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/geolocations")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.geolocationDTOList").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_embedded.geolocationDTOList[*].id").isNotEmpty());
    }

    @Test
    public void getGeolocations_NoContent() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/geolocations")
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    public void getGeolocationById() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Rte de Moutier", "14", "2800", "Delémont", "Switzerland"));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/geolocations/" + geo.getId())
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("streetName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("streetName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("streetName").value(geo.getStreetName()))
                .andExpect(MockMvcResultMatchers.jsonPath("streetNumber").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("streetNumber").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("streetNumber").value(geo.getStreetNumber()))
                .andExpect(MockMvcResultMatchers.jsonPath("postcode").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("postcode").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("postcode").value(geo.getPostcode()))
                .andExpect(MockMvcResultMatchers.jsonPath("locality").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("locality").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("locality").value(geo.getLocality()))
                .andExpect(MockMvcResultMatchers.jsonPath("country").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("country").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("country").value(geo.getCountry()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.geolocations.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.geolocations.href").isNotEmpty());
    }

    @Test
    public void getGeolocationById_WrongID() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                        .get("/api/geolocations/" + 99L)
                        .header("Authorization", token)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createGeo() throws Exception {
        GeolocationCuDTO geo = new GeolocationCuDTO(); geo.setStreetName("Ratapihantie"); geo.setStreetNumber("13");
        geo.setPostcode("00250"); geo.setLocality("Helsinki"); geo.setCountry("Finland");
        String requestBody = new Gson().toJson(geo);

        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("streetName").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("streetName").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("streetName").value(geo.getStreetName()))
                .andExpect(MockMvcResultMatchers.jsonPath("streetNumber").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("streetNumber").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("streetNumber").value(geo.getStreetNumber()))
                .andExpect(MockMvcResultMatchers.jsonPath("postcode").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("postcode").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("postcode").value(geo.getPostcode()))
                .andExpect(MockMvcResultMatchers.jsonPath("locality").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("locality").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("locality").value(geo.getLocality()))
                .andExpect(MockMvcResultMatchers.jsonPath("country").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("country").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("country").value(geo.getCountry()))
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.self.href").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.geolocations.href").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("_links.geolocations.href").isNotEmpty());
    }

    @Test
    public void createGeo_NullValue() throws Exception {
        GeolocationCuDTO geo = new GeolocationCuDTO(); geo.setStreetName("Ratapihantie"); geo.setStreetNumber("13");
        geo.setPostcode("00250"); geo.setLocality("Helsinki"); geo.setCountry("Finland");
        String requestBody;
        Gson gson = new Gson();

        geo.setStreetName(null);
        requestBody = gson.toJson(geo);
        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        geo.setStreetName("Ratapihantie");

        geo.setStreetNumber(null);
        requestBody = gson.toJson(geo);
        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        geo.setStreetNumber("13");

        geo.setPostcode(null);
        requestBody = gson.toJson(geo);
        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        geo.setPostcode("00250");

        geo.setLocality(null);
        requestBody = gson.toJson(geo);
        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        geo.setLocality("Helsinki");

        geo.setCountry(null);
        requestBody = gson.toJson(geo);
        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        geo.setCountry("Switzerland");
    }

    @Test
    public void createGeo_EmptyValue() throws Exception {
        GeolocationCuDTO geo = new GeolocationCuDTO(); geo.setStreetName("Ratapihantie"); geo.setStreetNumber("13");
        geo.setPostcode("00250"); geo.setLocality("Helsinki"); geo.setCountry("Finland");
        String requestBody;
        Gson gson = new Gson();

        geo.setStreetName("");
        requestBody = gson.toJson(geo);
        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        geo.setStreetName("Ratapihantie");

        geo.setStreetNumber("");
        requestBody = gson.toJson(geo);
        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        geo.setStreetNumber("13");

        geo.setPostcode("");
        requestBody = gson.toJson(geo);
        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        geo.setPostcode("00250");

        geo.setLocality("");
        requestBody = gson.toJson(geo);
        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        geo.setLocality("Helsinki");

        geo.setCountry("");
        requestBody = gson.toJson(geo);
        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
        geo.setCountry("Switzerland");
    }

    @Test
    public void createGeo_AlreadyExists() throws Exception {
        GeolocationCuDTO geo = new GeolocationCuDTO(); geo.setStreetName("Ratapihantie"); geo.setStreetNumber("13");
        geo.setPostcode("00250"); geo.setLocality("Helsinki"); geo.setCountry("Finland");
        String requestBody = new Gson().toJson(geo);

        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isCreated());

        mvc.perform(MockMvcRequestBuilders.post("/api/geolocations").accept(MediaType.APPLICATION_JSON).content(requestBody)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    public void deleteGeolocation() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Campus Battelle, Rue de la Tambourine", "17", "1227", "Carouge", "Switzerland"));

        mvc.perform(MockMvcRequestBuilders.delete("/api/geolocations/" + geo.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteGeolocation_WrongId() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/geolocations/" + 999L).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteGeolocation_Conflict() throws Exception {
        Geolocation geo = geoRepository.save(new Geolocation("Campus Battelle, Rue de la Tambourine", "17", "1227", "Carouge", "Switzerland"));
        supplierRepository.save(new Supplier("Tester", "tester@gmail.com", null, geo));
        customerRepository.save(new Customer("Paul", "Bocuz", "paul@bocuz.fi"));

        mvc.perform(MockMvcRequestBuilders.delete("/api/geolocations/" + geo.getId()).accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON).header("Authorization", token).with(csrf()))
                .andExpect(status().isConflict());
    }
}