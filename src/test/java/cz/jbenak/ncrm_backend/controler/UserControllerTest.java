package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@link UserController} verifying that user administration is restricted
 * to the owner while every authenticated user can read their own profile.
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class UserControllerTest {

    private static final String VALID_USER = """
            {
              "username": "john",
              "email": "john@example.com",
              "password": "secret-password",
              "firstName": "John",
              "lastName": "Doe",
              "enabled": true,
              "locked": false,
              "mustChangePassword": false,
              "sendCredentials": false,
              "roles": ["SALES_REPRESENTATIVE"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanListUsers() throws Exception {
        when(userService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void everyAuthenticatedUserCanReadOwnProfile() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCanListRepresentatives() throws Exception {
        when(userService.findAllSalesRepresentatives()).thenReturn(List.of());
        mockMvc.perform(get("/api/users/sales-representatives"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanCreateUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCannotCreateUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void createIsRejectedWithoutMandatoryFields() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotLockAccounts() throws Exception {
        mockMvc.perform(post("/api/users/{id}/locked/{locked}", UUID.randomUUID(), true))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanEnableAccounts() throws Exception {
        mockMvc.perform(post("/api/users/{id}/enabled/{enabled}", UUID.randomUUID(), true))
                .andExpect(status().isOk());
    }
}
