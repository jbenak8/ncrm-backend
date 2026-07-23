package cz.jbenak.ncrm_backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-23
 * Issues short-lived HS256-signed JWT access tokens for the "db-auth" profile, where the application
 * itself authenticates users against the database instead of Keycloak. The issued tokens carry the
 * same claims as Keycloak tokens ("preferred_username" and "realm_access.roles"), so the existing
 * JwtAuthenticationConverter and the React frontend work unchanged with both authentication back ends.
 */
@Service
@Profile("db-auth")
public class TokenService {

    /** Issuer identifying tokens minted by this application (not by Keycloak). */
    public static final String ISSUER = "ncrm-backend";

    private final JwtEncoder jwtEncoder;
    private final Duration tokenTtl;

    public TokenService(JwtEncoder jwtEncoder,
                        @Value("${ncrm.security.jwt.ttl:PT1H}") Duration tokenTtl) {
        this.jwtEncoder = jwtEncoder;
        this.tokenTtl = tokenTtl;
    }

    /**
     * Issues a signed access token for the authenticated user. Authorities with the "ROLE_" prefix
     * are stored without the prefix in the "realm_access.roles" claim (Keycloak-compatible format).
     */
    public IssuedToken issue(String username, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(tokenTtl))
                .claim("preferred_username", username)
                .claim("realm_access", Map.of("roles", roles))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(tokenValue, tokenTtl.toSeconds());
    }

    /** An issued access token together with its validity in seconds. */
    public record IssuedToken(String tokenValue, long expiresInSeconds) {
    }
}
