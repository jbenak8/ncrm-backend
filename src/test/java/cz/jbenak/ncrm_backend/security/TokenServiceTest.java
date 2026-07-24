package cz.jbenak.ncrm_backend.security;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests of {@link TokenService} verifying that the issued HS256 tokens can be validated
 * by the decoder of the "db-auth" profile and carry Keycloak-compatible claims, so the existing
 * JwtAuthenticationConverter maps them to the same authorities as Keycloak tokens.
 */
class TokenServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final SecurityConfig securityConfig = new SecurityConfig();
    private final TokenService tokenService =
            new TokenService(securityConfig.jwtEncoder(SECRET), Duration.ofMinutes(30));

    @Test
    void issuedTokenIsAcceptedByDecoderAndCarriesKeycloakCompatibleClaims() {
        TokenService.IssuedToken token = tokenService.issue("owner",
                List.of(new SimpleGrantedAuthority("ROLE_OWNER"), new SimpleGrantedAuthority("ROLE_ADMIN")));

        JwtDecoder decoder = securityConfig.jwtDecoder(SECRET);
        Jwt jwt = decoder.decode(token.tokenValue());

        assertThat(token.expiresInSeconds()).isEqualTo(Duration.ofMinutes(30).toSeconds());
        assertThat(jwt.getClaimAsString("preferred_username")).isEqualTo("owner");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(TokenService.ISSUER);

        AbstractAuthenticationToken authentication =
                securityConfig.keycloakJwtAuthenticationConverter().convert(jwt);
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("owner");
        assertThat(authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_")))
                .containsExactlyInAnyOrder("ROLE_OWNER", "ROLE_ADMIN");
    }

    @Test
    void nonRoleAuthoritiesAreNotIncludedInTheToken() {
        TokenService.IssuedToken token = tokenService.issue("owner",
                List.of(new SimpleGrantedAuthority("SCOPE_read"), new SimpleGrantedAuthority("ROLE_OWNER")));

        Jwt jwt = securityConfig.jwtDecoder(SECRET).decode(token.tokenValue());
        AbstractAuthenticationToken authentication =
                securityConfig.keycloakJwtAuthenticationConverter().convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_")))
                .containsExactly("ROLE_OWNER");
    }

    @Test
    void tooShortSecretIsRejected() {
        assertThatThrownBy(() -> securityConfig.jwtEncoder("too-short"))
                .hasMessageContaining("at least 32 bytes");
        assertThatThrownBy(() -> securityConfig.jwtDecoder("too-short"))
                .hasMessageContaining("at least 32 bytes");
    }
}
