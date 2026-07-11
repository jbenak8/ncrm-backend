package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.services.MeetingService;
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
 * Integration tests of {@link MeetingController} verifying that meetings are accessible
 * only to the owner and sales representatives.
 */
@WebMvcTest(MeetingController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeetingService meetingService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/meetings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotAccessMeetings() throws Exception {
        mockMvc.perform(get("/api/meetings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCanListMeetings() throws Exception {
        when(meetingService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/meetings"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void createValidatesRequestBody() throws Exception {
        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void completeStoresMeetingMinutes() throws Exception {
        mockMvc.perform(post("/api/meetings/{id}/complete", UUID.randomUUID())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Meeting minutes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanCancelMeeting() throws Exception {
        mockMvc.perform(post("/api/meetings/{id}/cancel", UUID.randomUUID()))
                .andExpect(status().isOk());
    }
}
