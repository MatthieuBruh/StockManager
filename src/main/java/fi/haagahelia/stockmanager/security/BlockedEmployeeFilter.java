package fi.haagahelia.stockmanager.security;

import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class BlockedEmployeeFilter extends OncePerRequestFilter {

    private final EmployeeRepository employeeRepository;

    public BlockedEmployeeFilter(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String username = request.getRemoteUser();
        Optional<Employee> employeeOptional = employeeRepository.findByUsername(username);

        if (employeeOptional.isPresent()) {
            Employee employee = employeeOptional.get();

            if (!employee.isEnabled() || !employee.isAccountNonLocked()) {
                request.setAttribute("errorMessage", "Employee's account is inactive or blocked.");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Employee's account is inactive or blocked.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
