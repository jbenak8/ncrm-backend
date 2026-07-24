package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.model.dto.ares.AresSubjectDto;
import cz.jbenak.ncrm_backend.services.AresService;
import cz.jbenak.ncrm_backend.services.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@link AresController} verifying that the ARES lookup is available
 * only to internal roles and missing subjects are reported as 404 problem details.
 */
@WebMvcTest(AresController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class AresControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AresService aresService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/ares/{registrationId}", "12345678"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotUseAresLookup() throws Exception {
        mockMvc.perform(get("/api/ares/{registrationId}", "12345678"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCanLookUpSubject() throws Exception {
        when(aresService.findByRegistrationId("12345678"))
                .thenReturn(new AresSubjectDto("12345678", "CZ12345678", "ACME s.r.o.", "112", null));

        mockMvc.perform(get("/api/ares/{registrationId}", "12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ACME s.r.o."));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void unknownSubjectIsTranslatedToNotFound() throws Exception {
        when(aresService.findByRegistrationId("00000000"))
                .thenThrow(new NotFoundException("Economic subject with registration id 00000000 not found in ARES"));

        mockMvc.perform(get("/api/ares/{registrationId}", "00000000"))
                .andExpect(status().isNotFound());
    }
}
