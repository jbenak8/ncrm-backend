package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.services.CountryService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@link CountryController} verifying that reading is available to any
 * authenticated user while modifications are restricted to the owner.
 */
@WebMvcTest(CountryController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class CountryControllerTest {

    private static final String VALID_COUNTRY = """
            {
              "isoCode": "CZE",
              "name": "Česko",
              "nameEn": "Czechia",
              "active": true,
              "euMember": true,
              "embargoed": false,
              "dialingCode": "420"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CountryService countryService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/countries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void anyAuthenticatedUserCanListCountries() throws Exception {
        when(countryService.findAll(false)).thenReturn(List.of());
        mockMvc.perform(get("/api/countries"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanCreateCountry() throws Exception {
        mockMvc.perform(post("/api/countries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_COUNTRY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCannotCreateCountry() throws Exception {
        mockMvc.perform(post("/api/countries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_COUNTRY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void createIsRejectedWithoutMandatoryFields() throws Exception {
        mockMvc.perform(post("/api/countries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotDeactivateCountry() throws Exception {
        mockMvc.perform(post("/api/countries/CZE/active/false"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanDeactivateCountry() throws Exception {
        mockMvc.perform(post("/api/countries/CZE/active/false"))
                .andExpect(status().isOk());
    }
}
