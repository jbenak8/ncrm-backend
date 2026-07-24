package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.services.CampaignService;
import cz.jbenak.ncrm_backend.services.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@link CampaignController} verifying that campaign management
 * is restricted to the owner role and errors are translated to problem details.
 */
@WebMvcTest(CampaignController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class CampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampaignService campaignService;

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanListCampaigns() throws Exception {
        when(campaignService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCannotAccessCampaigns() throws Exception {
        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void createValidatesRequestBody() throws Exception {
        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"subject\":\"\",\"body\":\"\",\"customerIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void missingCampaignIsTranslatedToNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(campaignService.findById(id)).thenThrow(new NotFoundException("Campaign", id));

        mockMvc.perform(get("/api/campaigns/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void alreadySentCampaignIsTranslatedToConflict() throws Exception {
        UUID id = UUID.randomUUID();
        when(campaignService.send(id)).thenThrow(new IllegalStateException("Campaign has already been sent"));

        mockMvc.perform(post("/api/campaigns/{id}/send", id))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void uploadContentReturnsExtractedText() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "campaign.txt", "text/plain", "Hello".getBytes());
        when(campaignService.extractContentFromFile(any())).thenReturn("Hello");

        mockMvc.perform(multipart("/api/campaigns/upload-content").file(file))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void invalidUploadIsTranslatedToBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "campaign.pdf", "application/pdf", new byte[0]);
        when(campaignService.extractContentFromFile(any()))
                .thenThrow(new IllegalArgumentException("Unable to read the uploaded file"));

        mockMvc.perform(multipart("/api/campaigns/upload-content").file(file))
                .andExpect(status().isBadRequest());
    }
}
