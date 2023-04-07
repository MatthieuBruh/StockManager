package fi.haagahelia.stockmanager.security;

import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import fi.haagahelia.stockmanager.security.jwt.JWTAuthenticationEntryPoint;
import fi.haagahelia.stockmanager.security.jwt.JWTAuthenticationFilter;
import fi.haagahelia.stockmanager.security.jwt.JWTUtils;
import fi.haagahelia.stockmanager.service.CustomEmployeeDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JWTUtils jwtUtils;
    private final JWTAuthenticationEntryPoint jwtAuthEntryPoint;
    private final CustomEmployeeDetailsService userDetailsService;
    private final EmployeeRepository employeeRepository;
    private final Environment environment;

    @Autowired
    public SecurityConfig(JWTUtils jwtUtils, JWTAuthenticationEntryPoint jwtAuthEntryPoint, CustomEmployeeDetailsService userDetailsService,
                          EmployeeRepository employeeRepository, Environment environment) {
        this.jwtUtils = jwtUtils;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.userDetailsService = userDetailsService;
        this.employeeRepository = employeeRepository;
        this.environment = environment;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConf) throws Exception {
        return authConf.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JWTAuthenticationFilter jwtAuthenticationFilter() {
        return new JWTAuthenticationFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public BlockedEmployeeFilter blockedUserFilter() {
        return new BlockedEmployeeFilter(employeeRepository);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(Objects.requireNonNull(environment.getProperty("spring.security.cors.allowed-origins")).split(",")));
        configuration.setAllowedMethods(Arrays.asList(Objects.requireNonNull(environment.getProperty("spring.security.cors.allowed-methods")).split(",")));
        configuration.setAllowedHeaders(Arrays.asList(Objects.requireNonNull(environment.getProperty("spring.security.cors.allowed-headers")).split(",")));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors(Customizer.withDefaults())
                .exceptionHandling().authenticationEntryPoint(jwtAuthEntryPoint)
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests()
                .requestMatchers(new AntPathRequestMatcher("/api/auth/login")).permitAll()
                .anyRequest().authenticated()
                .and()
                .headers().frameOptions().disable()
                .and()
                .httpBasic();

        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(blockedUserFilter(), RequestCacheAwareFilter.class);
        return http.build();
    }

}
