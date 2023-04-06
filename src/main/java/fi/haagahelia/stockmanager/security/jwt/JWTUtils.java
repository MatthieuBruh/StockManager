package fi.haagahelia.stockmanager.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

@Component
public class JWTUtils {

    public static String generateToken(String subject) {
        Date currentDate = new Date();
        Calendar calendarDate = Calendar.getInstance();
        calendarDate.setTime(currentDate);
        calendarDate.add(JWTConstants.JWT_EXPIRATION_UNIT, JWTConstants.JWT_EXPIRATION_DURATION);

        Date expireDate = calendarDate.getTime();

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS512, JWTConstants.JWT_SECRET)
                .compact();
    }

    public String getUsernameFromJwt(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(JWTConstants.JWT_SECRET)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(JWTConstants.JWT_SECRET).parseClaimsJws(token);

            return true;
        } catch (Exception e) {
            throw new AuthenticationCredentialsNotFoundException("JWT_EXPIRED_OR_INCORRECT");
        }
    }
}
