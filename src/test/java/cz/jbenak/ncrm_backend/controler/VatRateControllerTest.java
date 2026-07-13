package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.services.VatRateService;
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
 * Integration tests of {@link VatRateController} verifying that reading is available to any
 * authenticated user while modifications are restricted to the owner.
 */
@WebMvcTest(VatRateController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class VatRateControllerTest {

    private static final String VALID_VAT_RATE = """
            {
              "countryIsoCode": "CZE",
              "type": "BASE",
              "rate": 21.00,
              "validFrom": "2026-01-01T00:00:00"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VatRateService vatRateService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/vat-rates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void anyAuthenticatedUserCanListVatRates() throws Exception {
        when(vatRateService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/vat-rates"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void listCanBeFilteredByCountry() throws Exception {
        when(vatRateService.findByCountry("CZE")).thenReturn(List.of());
        mockMvc.perform(get("/api/vat-rates").param("countryIsoCode", "CZE"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanCreateVatRate() throws Exception {
        mockMvc.perform(post("/api/vat-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VAT_RATE))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotCreateVatRate() throws Exception {
        mockMvc.perform(post("/api/vat-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_VAT_RATE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void createIsRejectedWithoutMandatoryFields() throws Exception {
        mockMvc.perform(post("/api/vat-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCannotDeleteVatRate() throws Exception {
        mockMvc.perform(delete("/api/vat-rates/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanDeleteVatRate() throws Exception {
        mockMvc.perform(delete("/api/vat-rates/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
