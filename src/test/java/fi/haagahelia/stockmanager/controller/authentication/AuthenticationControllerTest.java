package fi.haagahelia.stockmanager.controller.authentication;

import com.google.gson.Gson;
import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.authentication.EmpChangePasswordDTO;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AuthenticationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    public void login() throws Exception {
        Role admin = new Role("ROLE_ADMIN", "For the admins"); roleRepository.save(admin);
        String password = "A1234";
        Employee employee = new Employee("main@haaga.fi", "main", "Main", "Haaga", new BCryptPasswordEncoder().encode(password), true, false);
        employeeRepository.save(employee);
        employee.setRoles(List.of(admin)); employeeRepository.save(employee);
        String requestBody = "{ \"username\": \"" + employee.getUsername() + "\", \"password\": \"" + password + "\"}";

        mvc.perform(MockMvcRequestBuilders
                        .post("/api/auth/login")
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("tokenType").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("tokenType").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("tokenType").value("Bearer "))
                .andExpect(MockMvcResultMatchers.jsonPath("token").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("token").isNotEmpty());
    }

    @Test
    public void login_WrongPassword() throws Exception {
        Role admin = new Role("ROLE_ADMIN", "For the admins"); roleRepository.save(admin);
        String password = "A1234";
        Employee employee = new Employee("main@haaga.fi", "main", "Main", "Haaga", new BCryptPasswordEncoder().encode(password), true, false);
        employeeRepository.save(employee);
        employee.setRoles(List.of(admin)); employeeRepository.save(employee);
        String requestBody = "{ \"username\": \"" + employee.getUsername() + "\", \"password\": \"" + "WRONG_PASSWORD" + "\"}";

        mvc.perform(MockMvcRequestBuilders
                        .post("/api/auth/login")
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("INVALID_USERNAME_OR_PASSWORD"));
    }

    @Test
    public void login_Blocked() throws Exception {
        Role admin = new Role("ROLE_ADMIN", "For the admins"); roleRepository.save(admin);
        String password = "A1234";
        Employee employee = new Employee("main@haaga.fi", "main", "Main", "Haaga", new BCryptPasswordEncoder().encode(password), false, true);
        employeeRepository.save(employee);
        employee.setRoles(List.of(admin)); employeeRepository.save(employee);
        String requestBody = "{ \"username\": \"" + employee.getUsername() + "\", \"password\": \"" + password + "\"}";

        mvc.perform(MockMvcRequestBuilders
                        .post("/api/auth/login")
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("message").value("ACCOUNT_IS_LOCKED"));
    }

    @Test
    public void changePassword() throws Exception {
        Role admin = new Role("ROLE_ADMIN", "For the admins"); roleRepository.save(admin);
        String password = "A1234";
        Employee employee = new Employee("main@haaga.fi", "main", "Main", "Haaga", new BCryptPasswordEncoder().encode(password), true, false);
        employeeRepository.save(employee);
        employee.setRoles(List.of(admin)); employeeRepository.save(employee);
        String requestBody = "{ \"username\": \"" + employee.getUsername() + "\", \"password\": \"" + password + "\"}";
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").accept(MediaType.APPLICATION_JSON).content(requestBody).header("Content-Type", MediaType.APPLICATION_JSON)).andReturn();
        String token;
        if (mvcResult.getResponse().getStatus() == 200) {
            AuthResponseDTO authResponseDTO = new Gson().fromJson(mvcResult.getResponse().getContentAsString(), AuthResponseDTO.class);
            token = "Bearer " + authResponseDTO.getToken();
        } else {
            throw new RuntimeException("AUTHENTICATION FAILED!!");
        }

        EmpChangePasswordDTO passwordDTO = new EmpChangePasswordDTO(); passwordDTO.setCurrentPassword(password);
        passwordDTO.setNewPassword("AB-123456"); passwordDTO.setNewPasswordVerification("AB-123456");
        requestBody = new Gson().toJson(passwordDTO);
        mvc.perform(MockMvcRequestBuilders
                        .put("/api/auth/password")
                        .header("Authorization", token)
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void changePassword_WrongCurrPassword() throws Exception {
        Role admin = new Role("ROLE_ADMIN", "For the admins"); roleRepository.save(admin);
        String password = "A1234";
        Employee employee = new Employee("main@haaga.fi", "main", "Main", "Haaga", new BCryptPasswordEncoder().encode(password), true, false);
        employeeRepository.save(employee);
        employee.setRoles(List.of(admin)); employeeRepository.save(employee);
        String requestBody = "{ \"username\": \"" + employee.getUsername() + "\", \"password\": \"" + password + "\"}";
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").accept(MediaType.APPLICATION_JSON).content(requestBody).header("Content-Type", MediaType.APPLICATION_JSON)).andReturn();
        String token;
        if (mvcResult.getResponse().getStatus() == 200) {
            AuthResponseDTO authResponseDTO = new Gson().fromJson(mvcResult.getResponse().getContentAsString(), AuthResponseDTO.class);
            token = "Bearer " + authResponseDTO.getToken();
        } else {
            throw new RuntimeException("AUTHENTICATION FAILED!!");
        }

        EmpChangePasswordDTO passwordDTO = new EmpChangePasswordDTO(); passwordDTO.setCurrentPassword("HEHE");
        passwordDTO.setNewPassword("AB-123456"); passwordDTO.setNewPasswordVerification("AB-123456");
        requestBody = new Gson().toJson(passwordDTO);
        mvc.perform(MockMvcRequestBuilders
                        .put("/api/auth/password")
                        .header("Authorization", token)
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void changePassword_InvalidData() throws Exception {
        Role admin = new Role("ROLE_ADMIN", "For the admins"); roleRepository.save(admin);
        String password = "A1234";
        Employee employee = new Employee("main@haaga.fi", "main", "Main", "Haaga", new BCryptPasswordEncoder().encode(password), true, false);
        employeeRepository.save(employee);
        employee.setRoles(List.of(admin)); employeeRepository.save(employee);
        String requestBody = "{ \"username\": \"" + employee.getUsername() + "\", \"password\": \"" + password + "\"}";
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").accept(MediaType.APPLICATION_JSON).content(requestBody).header("Content-Type", MediaType.APPLICATION_JSON)).andReturn();
        String token;
        if (mvcResult.getResponse().getStatus() == 200) {
            AuthResponseDTO authResponseDTO = new Gson().fromJson(mvcResult.getResponse().getContentAsString(), AuthResponseDTO.class);
            token = "Bearer " + authResponseDTO.getToken();
        } else {
            throw new RuntimeException("AUTHENTICATION FAILED!!");
        }

        EmpChangePasswordDTO passwordDTO = new EmpChangePasswordDTO(); passwordDTO.setCurrentPassword(password);
        passwordDTO.setNewPassword("AB-123456"); passwordDTO.setNewPasswordVerification("AB-1234");
        requestBody = new Gson().toJson(passwordDTO);
        mvc.perform(MockMvcRequestBuilders
                        .put("/api/auth/password")
                        .header("Authorization", token)
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isPreconditionFailed());

        passwordDTO = new EmpChangePasswordDTO(); passwordDTO.setCurrentPassword(password);
        passwordDTO.setNewPassword("AB-12"); passwordDTO.setNewPasswordVerification("AB-12");
        requestBody = new Gson().toJson(passwordDTO);
        mvc.perform(MockMvcRequestBuilders
                        .put("/api/auth/password")
                        .header("Authorization", token)
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isPreconditionFailed());
    }
}