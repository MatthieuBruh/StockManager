package fi.haagahelia.stockmanager.controller.authentication;


import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.authentication.EmpChangePasswordDTO;
import fi.haagahelia.stockmanager.dto.authentication.EmpLoginDTO;
import fi.haagahelia.stockmanager.dto.common.ErrorResponse;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import fi.haagahelia.stockmanager.security.jwt.JWTAuthenticationEntryPoint;
import fi.haagahelia.stockmanager.security.jwt.JWTUtils;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Log4j2
public class AuthenticationController {

    private final AuthenticationManager authMan;
    private final EmployeeRepository eRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public AuthenticationController(AuthenticationManager authMan, EmployeeRepository eRepository, BCryptPasswordEncoder passwordEncoder) {
        this.authMan = authMan;
        this.eRepository = eRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * This method is used to handle the login request for a user.
     *
     * @param loginDTO object as input, which represents the user's login credentials.
     * @return a ResponseEntity containing an AuthResponse or an ErrorResponse.
     *      --> HttpStatus.OK if authentication is successful
     *      --> HttpStatus.UNAUTHORIZED if authentication fails
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error happens.
     */
    @PostMapping(value = "/login")
    public ResponseEntity<?> login(@RequestBody @Valid EmpLoginDTO loginDTO) {
        log.info("Authentication for the user: '{}'.", loginDTO.getUsername());
        try {
            if (!eRepository.existsByUsername(loginDTO.getUsername())) throw new BadCredentialsException("INVALID_USERNAME_OR_PASSWORD");
            Authentication auth = authMan.authenticate(new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(auth);
            String token = JWTUtils.generateToken(auth.getName());
            AuthResponseDTO responseDTO = new AuthResponseDTO(token);
            log.info("Authentication successful for the user: '{}'.", loginDTO.getUsername());
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (AuthenticationException e) {
            log.info("Authentication failed for the user: '{}'. {}", loginDTO.getUsername(), e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.getReasonPhrase(), JWTAuthenticationEntryPoint.getErrorMessage(e));
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.info("Authentication failed for the user: '{}'. UNEXPECTED ERROR ({}).", loginDTO.getUsername(), e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "UNEXPECTED ERROR");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * This method is used to update an employee password.
     * Firstly, we check that the given password are valid, not null, minimal length, and corresponding.
     *      If not, we return an HttpStatus.PRECONDITION_FAILED to the user.
     * Secondly, we verify that the given current password is the same that the one in the database.
     *      If not, we return an HttpStatus.BAD_REQUEST to the user.
     * Thirdly, we search the user in the database, and we apply the new password.
     * Finally, we save the new password, and we return an HttpStatus.OK to the user.
     *
     * @param user correspond to the authenticated user.
     * @param empPassword correspond to the user's password, current and new.
     * @return a ResponseEntity containing a http code and depending on the result an error message.
     *      --> HttpStatus.OK if the password has been updated correctly.
     *      --> HttpStatus.PRECONDITION_FAILED if the given passwords are null, too short, or not the same. (ErrorMessage)
     *      --> HttpStatus.BAD_REQUEST if the given current password correspond to the one in the database. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @PutMapping(value = "/password")
    @PreAuthorize("hasAnyRole('ROLE_VENDOR', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal Employee user, @RequestBody EmpChangePasswordDTO empPassword) {
        log.info("User '{}' is requesting to change her/his password.", user.getUsername());
        try {
            if (!empPassword.isValid()) {
                log.info("User '{}' is requesting to change her/his password. A GIVEN PASSWORD IS INVALID", user.getUsername());
                ErrorResponse errorResponse = new ErrorResponse(HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), "INVALID_DATA");
                return new ResponseEntity<>(errorResponse, HttpStatus.PRECONDITION_FAILED);
            }
            if (!passwordEncoder.matches(empPassword.getCurrentPassword(), user.getPassword())) {
                log.info("User '{}' is requesting to change her/his password. CURRENT PASSWORD NOT CORRECT", user.getUsername());
                ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "INVALID_CURRENT_PASSWORD");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
            Optional<Employee> employeeOptional = eRepository.findByUsername(user.getUsername());
            if (employeeOptional.isEmpty()) {
                throw new Exception("UNEXPECTED DATABASE MODIFICATION! Account not found, cannot modify password.");
            }
            Employee employee = employeeOptional.get();
            employee.setPassword(passwordEncoder.encode(empPassword.getNewPassword()));
            eRepository.save(employee);
            log.info("User '{}' is requesting to change her/his password.", user.getUsername());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.info("User '{}' is requesting to change her/his password. UNEXPECTED ERROR ({}).", user.getUsername(), e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "UNEXPECTED ERROR");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
