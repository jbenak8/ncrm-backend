package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.model.dto.security.UserDto;
import cz.jbenak.ncrm_backend.security.TokenService;
import cz.jbenak.ncrm_backend.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@link AuthController} verifying the database-backed login of the "db-auth"
 * profile: valid credentials yield a signed JWT access token, invalid or locked accounts are
 * rejected with HTTP 401 without leaking any details.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, TokenService.class})
@ActiveProfiles("db-auth")
@TestPropertySource(properties = {
        "ncrm.security.jwt.secret=0123456789abcdef0123456789abcdef",
        "ncrm.security.jwt.ttl=PT30M"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUpUsers() {
        String hash = passwordEncoder.encode("Secret-123");
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new UsernameNotFoundException("User not found."));
        when(userDetailsService.loadUserByUsername("owner")).thenReturn(User.withUsername("owner")
                .password(hash)
                .roles("OWNER")
                .build());
        when(userDetailsService.loadUserByUsername("locked")).thenReturn(User.withUsername("locked")
                .password(hash)
                .roles("CUSTOMER")
                .accountLocked(true)
                .build());
        when(userService.findByUsername("owner")).thenReturn(new UserDto(UUID.randomUUID(), "owner",
                "owner@example.com", "Otto", "Owner", true, false, false, true, null,
                Set.of("OWNER"), Set.of(), null));
    }

    private static String loginBody(String username, String password) {
        return """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password);
    }

    @Test
    void validCredentialsYieldAccessToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("owner", "Secret-123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("owner", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownUserIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("ghost", "Secret-123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lockedAccountIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("locked", "Secret-123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingCredentialsAreRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
