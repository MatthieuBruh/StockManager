package fi.haagahelia.stockmanager.controller.authentication;


import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.authentication.EmpLoginDTO;
import fi.haagahelia.stockmanager.dto.common.ErrorResponse;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import fi.haagahelia.stockmanager.security.jwt.JWTAuthenticationEntryPoint;
import fi.haagahelia.stockmanager.security.jwt.JWTUtils;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Log4j2
public class AuthenticationController {

    private final AuthenticationManager authMan;
    private final EmployeeRepository eRepository;

    @Autowired
    public AuthenticationController(AuthenticationManager authMan, EmployeeRepository eRepository) {
        this.authMan = authMan;
        this.eRepository = eRepository;
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
        log.info("Authentication for the user: {}.", loginDTO.getUsername());
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
}
