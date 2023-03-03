package fi.haagahelia.stockmanager.security.jwt;

import fi.haagahelia.stockmanager.security.SecurityUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

@Component
public class JWTUtils {

    private static final String JWT_SECRET = SecurityUtils.jwtSecret;
    private static final int EXPIRATION_DURATION = SecurityUtils.jwtExpiration;
    private static final int EXPIRATION_UNIT = SecurityUtils.jwtExpirationUnit;

    public static String generateToken(String subject) {
        Date currentDate = new Date();

        Calendar calendarDate = Calendar.getInstance();
        calendarDate.setTime(currentDate);
        calendarDate.add(EXPIRATION_UNIT, EXPIRATION_DURATION);

        Date expireDate = calendarDate.getTime();

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS512, JWT_SECRET)
                .compact();
    }

    public String getUsernameFromJwt(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(JWT_SECRET)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(JWT_SECRET).parseClaimsJws(token);

            return true;
        } catch (Exception e) {
            throw new AuthenticationCredentialsNotFoundException("JWT_EXPIRED_OR_INCORRECT");
        }
    }
}
