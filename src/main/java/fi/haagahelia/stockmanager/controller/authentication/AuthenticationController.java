package fi.haagahelia.stockmanager.controller.authentication;


import fi.haagahelia.stockmanager.dto.authentication.AuthResponseDTO;
import fi.haagahelia.stockmanager.dto.authentication.EmpLoginDTO;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import fi.haagahelia.stockmanager.repository.user.RoleRepository;
import fi.haagahelia.stockmanager.security.JwtGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthenticationController {

    private AuthenticationManager authenticationManager;
    private JwtGenerator jwtGenerator;

    @Autowired
    public AuthenticationController(AuthenticationManager authenticationManager, JwtGenerator jwtGenerator) {
        this.authenticationManager = authenticationManager;
        this.jwtGenerator = jwtGenerator;
    }


    @PostMapping(value = "/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody EmpLoginDTO loginDTO) {
        log.info("Authentication: {} ; {}", loginDTO.getUsername(), loginDTO.getPassword());

        System.out.println("========== ICI ==========");
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword()));

        System.out.println("========== ICI1 ==========");

        SecurityContextHolder.getContext().setAuthentication(authentication);

        System.out.println("========== ICI2 ==========");

        String token = jwtGenerator.generateToken(authentication);

        System.out.println("========== ICI3 ==========");

        return new ResponseEntity<>(new AuthResponseDTO(token), HttpStatus.OK);
    }
}
