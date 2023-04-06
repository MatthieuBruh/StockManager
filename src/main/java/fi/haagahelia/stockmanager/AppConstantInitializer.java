package fi.haagahelia.stockmanager;

import fi.haagahelia.stockmanager.security.jwt.JWTConstants;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Objects;

@Configuration
public class AppConstantInitializer {

    private final Environment env;

    @Autowired
    public AppConstantInitializer(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void init() {
        JWTConstants.JWT_EXPIRATION_DURATION = Objects.requireNonNull(env.getProperty("jwt.expiration.duration", Integer.class));
        JWTConstants.JWT_EXPIRATION_UNIT = Objects.requireNonNull(env.getProperty("jwt.expiration.unit", Integer.class));
        JWTConstants.JWT_SECRET = Objects.requireNonNull(env.getProperty("jwt.secret"));
    }
}
