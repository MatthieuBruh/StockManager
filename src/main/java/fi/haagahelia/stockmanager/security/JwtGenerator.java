package fi.haagahelia.stockmanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

@Component
public class JwtGenerator {

    public String generateToken(Authentication auth) {
        String username = auth.getName();
        Date currentDate = new Date();

        Calendar calendarDate = Calendar.getInstance();
        calendarDate.setTime(currentDate);
        calendarDate.add(SecurityConstants.jwtExpirationUnit, SecurityConstants.jwtExpiration);

        Date expireDate = calendarDate.getTime();

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS512, SecurityConstants.jwtSecret)
                .compact();
    }

    public String getUsernameFromJwt(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(SecurityConstants.jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SecurityConstants.jwtSecret).parseClaimsJws(token);

            return true;
        } catch (Exception e) {
            throw new AuthenticationCredentialsNotFoundException("Jwt was expired or incorrect.");
        }
    }
}
