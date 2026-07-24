package cz.jbenak.ncrm_backend.configuration;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Security configuration of the REST API consumed by the React frontend.
 * <p>
 * Profiles:
 * - "prod": the API acts as an OAuth2 resource server validating JWT access tokens issued by Keycloak.
 *   Keycloak also brokers external identity providers (Google, Bank ID, ...) and handles user registration.
 *   Realm roles from the "realm_access" claim are mapped to Spring Security ROLE_* authorities.
 * - "db-auth": self-contained alternative to Keycloak. Users authenticate with the credentials stored
 *   in the application database (BCrypt hashes in the "users" table) via POST /api/auth/login, which
 *   returns an HS256-signed JWT carrying Keycloak-compatible claims; the API then validates these
 *   tokens as a resource server using a shared HMAC secret.
 * - "local": in-memory test users (owner / rep / customer, password "test") with HTTP Basic for local testing
 *   without a running Keycloak instance.
 * <p>
 * Security principles applied: stateless sessions, least privilege via method security (@PreAuthorize),
 * CSRF disabled only because the API is stateless and token-based, restrictive CORS, standard security headers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${ncrm.security.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    /**
     * Production chain: JWT resource server backed by Keycloak.
     */
    @Bean
    @Profile("prod")
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF protection is not needed: the API is stateless and authenticated by bearer JWTs,
                // no session cookies are used, so cross-site request forgery is not applicable.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // Swagger / OpenAPI documentation of the REST API.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)));
        return http.build();
    }

    /**
     * Database authentication chain: users log in with credentials stored in the application database
     * via POST /api/auth/login and receive an HS256-signed JWT; all other API requests are validated
     * as bearer tokens using the shared HMAC secret. No Keycloak instance is needed.
     */
    @Bean
    @Profile("db-auth")
    public SecurityFilterChain dbAuthSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF protection is not needed: the API is stateless and authenticated by bearer JWTs,
                // no session cookies are used, so cross-site request forgery is not applicable.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // Swagger / OpenAPI documentation of the REST API.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Login endpoint issuing the JWT access tokens.
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder)
                                .jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)));
        return http.build();
    }

    /**
     * Authentication manager for the db-auth login endpoint, checking the submitted credentials
     * against the BCrypt password hashes stored in the application database.
     */
    @Bean
    @Profile("db-auth")
    public AuthenticationManager dbAuthenticationManager(UserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Encoder signing the issued access tokens with the shared HMAC secret (HS256).
     */
    @Bean
    @Profile("db-auth")
    public JwtEncoder jwtEncoder(@Value("${ncrm.security.jwt.secret}") String jwtSecret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(hmacKey(jwtSecret)));
    }

    /**
     * Decoder validating the HS256 signature of the access tokens issued by this application.
     */
    @Bean
    @Profile("db-auth")
    public JwtDecoder jwtDecoder(@Value("${ncrm.security.jwt.secret}") String jwtSecret) {
        return NimbusJwtDecoder.withSecretKey(hmacKey(jwtSecret))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Builds the HMAC-SHA256 key from the configured secret; at least 32 bytes are required for HS256. */
    private static SecretKey hmacKey(String jwtSecret) {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("ncrm.security.jwt.secret must be at least 32 bytes long for HS256");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * Local testing chain: HTTP Basic with in-memory users, no Keycloak needed.
     */
    @Bean
    @Profile("local")
    public SecurityFilterChain localSecurityFilterChain(HttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF protection is not needed: stateless HTTP Basic API used only for local testing.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        // Swagger / OpenAPI documentation of the REST API.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(basic -> {
                });
        return http.build();
    }

    /**
     * In-memory test users for the local profile.
     */
    @Bean
    @Profile("local")
    public UserDetailsService localUsers(PasswordEncoder passwordEncoder,
                                         @Value("${ncrm.security.local-users.password}") String localUsersPassword) {
        String password = passwordEncoder.encode(localUsersPassword);
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password(password).roles("ADMIN").build(),
                User.withUsername("owner").password(password).roles("OWNER").build(),
                User.withUsername("rep").password(password).roles("SALES_REPRESENTATIVE").build(),
                User.withUsername("customer").password(password).roles("CUSTOMER").build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Maps Keycloak realm roles ("realm_access.roles") to Spring Security ROLE_* authorities.
     */
    @Bean
    public JwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::extractRealmRoles);
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    @SuppressWarnings("unchecked")
    private static Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(Object::toString)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toSet());
    }

    /**
     * Restrictive CORS configuration allowing only the configured frontend origins.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
