package cz.jbenak.ncrm_backend.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests of {@link SecurityConfig} verifying the mapping of Keycloak realm roles
 * from the "realm_access" JWT claim to Spring Security ROLE_* authorities.
 */
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    // Spring Security automatically adds a FACTOR_BEARER authority; only ROLE_* authorities are relevant here.
    private static List<String> roleAuthorities(AbstractAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("preferred_username", "owner");
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    void realmRolesAreMappedToRoleAuthorities() {
        JwtAuthenticationConverter converter = securityConfig.keycloakJwtAuthenticationConverter();
        Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of("owner", "SALES_REPRESENTATIVE"))));

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("owner");
        assertThat(roleAuthorities(authentication))
                .containsExactlyInAnyOrder("ROLE_OWNER", "ROLE_SALES_REPRESENTATIVE");
    }

    @Test
    void missingRealmAccessYieldsNoAuthorities() {
        JwtAuthenticationConverter converter = securityConfig.keycloakJwtAuthenticationConverter();

        AbstractAuthenticationToken authentication = converter.convert(jwt(Map.of()));

        assertThat(authentication).isNotNull();
        assertThat(roleAuthorities(authentication)).isEmpty();
    }

    @Test
    void malformedRolesClaimYieldsNoAuthorities() {
        JwtAuthenticationConverter converter = securityConfig.keycloakJwtAuthenticationConverter();
        Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", "not-a-collection")));

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(roleAuthorities(authentication)).isEmpty();
    }

    @Test
    void passwordEncoderUsesBcrypt() {
        String encoded = securityConfig.passwordEncoder().encode("secret");

        assertThat(securityConfig.passwordEncoder().matches("secret", encoded)).isTrue();
    }
}
