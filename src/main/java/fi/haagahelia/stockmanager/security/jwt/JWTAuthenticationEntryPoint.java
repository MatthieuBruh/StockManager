package fi.haagahelia.stockmanager.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.haagahelia.stockmanager.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JWTAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        String errorMessage = (String) request.getAttribute("errorMessage");
        if (errorMessage == null) {
            errorMessage = getErrorMessage(authException);
        }

        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.getReasonPhrase(), errorMessage);
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
    }

    public static String getErrorMessage(AuthenticationException authException) {
        if (authException instanceof BadCredentialsException) {
            return "INVALID_USERNAME_OR_PASSWORD";
        } else if (authException instanceof LockedException) {
            return "ACCOUNT_IS_LOCKED";
        } else {
            return "AUTHENTICATION_FAILED";
        }
    }
}
