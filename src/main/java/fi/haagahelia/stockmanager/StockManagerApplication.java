package fi.haagahelia.stockmanager;

import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.model.user.Role;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import fi.haagahelia.stockmanager.repository.user.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

@SpringBootApplication
public class StockManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockManagerApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(RoleRepository rRepository, EmployeeRepository eRepository) {
        return args -> {
            /*
            Role admin = new Role("Admin", "For the admins"); rRepository.save(admin);
            Role manager = new Role("Manager", "For the managers");  rRepository.save(manager);
            Role vendor = new Role("Vendors", "For the vendors"); rRepository.save(vendor);

            Employee main = new Employee("main@haaga.fi", "main", "Main", "Haaga",
                    new BCryptPasswordEncoder().encode("A1234"), true, false);
            eRepository.save(main);
            main.setRoles(List.of(admin));
            eRepository.save(main);


            Employee man = new Employee("man@haaga.fi", "man", "Man", "Haaga",
                    new BCryptPasswordEncoder().encode("A1234"), true, false);
            eRepository.save(man);

            Employee ven = new Employee("ven@haaga.fi", "ven", "Ven", "Haaga",
                    new BCryptPasswordEncoder().encode("A1234"), true, false);
            eRepository.save(ven);
            */
        };
    }

}
