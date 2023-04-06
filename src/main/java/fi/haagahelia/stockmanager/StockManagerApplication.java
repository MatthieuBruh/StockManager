package fi.haagahelia.stockmanager;

import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import fi.haagahelia.stockmanager.repository.user.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
public class StockManagerApplication {

    private final Environment environment;

    @Autowired
    public StockManagerApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(StockManagerApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(RoleRepository rRepository, EmployeeRepository eRepository) {
        return args -> {
            String[] activeProfiles = environment.getActiveProfiles();
            if (!Arrays.asList(activeProfiles).contains("prod")) return;

            // Ensuring that the 3 MAIN ROLES EXISTS
            Optional<Role> adminRoleOptional = rRepository.findByName("ROLE_ADMIN");
            Role adminRole;
            if (adminRoleOptional.isEmpty()) {
                Role newAdminRole = new Role("ROLE_ADMIN", "Role for administrators. LIMIT ACCESS TO THIS ROLE.");
                adminRole = rRepository.save(newAdminRole);
            } else {
                adminRole = adminRoleOptional.get();
            }

            if (!rRepository.existsByName("ROLE_MANAGER")) rRepository.save(new Role("ROLE_MANAGER", "Role for managers."));

            if (!rRepository.existsByName("ROLE_VENDOR")) rRepository.save(new Role("ROLE_VENDOR", "Role for vendors."));


            // Creating a main account. Password MUST BE CHANGED with a strong one!!
            // Create a new admin account and deactivate the default admin account!!
            // DO NOT DELETE THE ACCOUNT, IT WILL BE RECREATED AUTOMATICALLY!!

            if (!eRepository.existsByUsername("main")) {
                Employee mainEmployee = new Employee("main@stockManagement.com", "main", "DO NOT USE",
                        "DO NOT USE", new BCryptPasswordEncoder().encode("A1234"), true, false);
                eRepository.save(mainEmployee);

                // Attributing ADMIN ROLE
                List<Role> roles = new ArrayList<>(); roles.add(adminRole);
                mainEmployee.setRoles(roles);
                eRepository.save(mainEmployee);
            }
        };
    }

}
