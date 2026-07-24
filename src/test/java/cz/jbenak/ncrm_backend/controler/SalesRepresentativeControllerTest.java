package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.services.SalesRepresentativeService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@link SalesRepresentativeController} verifying that the administration
 * of sales representatives is restricted to the owner while a representative can read details.
 */
@WebMvcTest(SalesRepresentativeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class SalesRepresentativeControllerTest {

    private static final String VALID_REPRESENTATIVE = """
            {
              "code": "REP1",
              "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "phone": "+420777888999",
              "region": "Praha",
              "active": true
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalesRepresentativeService salesRepresentativeService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/sales-representatives"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanListRepresentatives() throws Exception {
        when(salesRepresentativeService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/sales-representatives"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCannotListAllRepresentatives() throws Exception {
        mockMvc.perform(get("/api/sales-representatives"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCanReadRepresentativeDetail() throws Exception {
        mockMvc.perform(get("/api/sales-representatives/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanCreateRepresentative() throws Exception {
        mockMvc.perform(post("/api/sales-representatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REPRESENTATIVE))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCannotCreateRepresentative() throws Exception {
        mockMvc.perform(post("/api/sales-representatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REPRESENTATIVE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void createIsRejectedWithoutMandatoryFields() throws Exception {
        mockMvc.perform(post("/api/sales-representatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanDeactivateRepresentative() throws Exception {
        mockMvc.perform(post("/api/sales-representatives/{id}/active/false", UUID.randomUUID()))
                .andExpect(status().isOk());
    }
}
