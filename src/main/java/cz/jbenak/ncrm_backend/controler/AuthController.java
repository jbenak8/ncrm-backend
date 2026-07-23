package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.model.dto.security.LoginRequest;
import cz.jbenak.ncrm_backend.model.dto.security.TokenResponse;
import cz.jbenak.ncrm_backend.security.TokenService;
import cz.jbenak.ncrm_backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-23
 * REST API for logging in with the credentials stored in the application database ("db-auth" profile).
 * The submitted username and password are verified against the BCrypt hashes in the "users" table;
 * on success an HS256-signed JWT access token with Keycloak-compatible claims is returned, so the
 * React frontend only needs to call this endpoint instead of the Keycloak token endpoint and can
 * keep sending the token as a standard "Authorization: Bearer ..." header.
 * Failed logins are translated to HTTP 401 by Spring Security; no details are leaked.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Profile("db-auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserService userService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        TokenService.IssuedToken token = tokenService.issue(authentication.getName(), authentication.getAuthorities());
        userService.recordLogin(authentication.getName());
        boolean mustChangePassword = userService.findByUsername(authentication.getName()).mustChangePassword();
        log.info("User {} logged in via database authentication", authentication.getName());
        return new TokenResponse(token.tokenValue(), "Bearer", token.expiresInSeconds(), mustChangePassword);
    }
}
