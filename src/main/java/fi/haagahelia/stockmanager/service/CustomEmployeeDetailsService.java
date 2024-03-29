package fi.haagahelia.stockmanager.service;

import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class CustomEmployeeDetailsService implements UserDetailsService {

    private final EmployeeRepository eRepository;

    @Autowired
    public CustomEmployeeDetailsService(EmployeeRepository eRepository) {
        this.eRepository = eRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            Optional<Employee> employeeOptional = eRepository.findByUsername(username);
            if (employeeOptional.isEmpty()) {
                throw new UsernameNotFoundException("No employee found with the username: " + username);
            }

            return employeeOptional.get();
        } catch (UsernameNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while retrieving user details", e);
        }
    }
}
