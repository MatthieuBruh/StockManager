package fi.haagahelia.stockmanager.service;

import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
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
        Optional<Employee> employeeOptional = eRepository.findByUsername(username);
        if (!employeeOptional.isPresent()) {
            throw new UsernameNotFoundException(MessageFormat.format("No employee found with the username: {}", username));
        }

        return employeeOptional.get();
    }
}
