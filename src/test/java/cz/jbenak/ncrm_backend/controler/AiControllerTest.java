package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.ai.AiService;
import cz.jbenak.ncrm_backend.configuration.SecurityConfig;
import cz.jbenak.ncrm_backend.model.dto.ai.AiDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of {@link AiController} verifying that the AI chat and content generation
 * endpoints are available only to internal roles and validate their payloads.
 */
@WebMvcTest(AiController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiService aiService;

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotUseAi() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hi\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SALES_REPRESENTATIVE")
    void salesRepresentativeCanChat() throws Exception {
        when(aiService.chat(any())).thenReturn(new AiDtos.ChatResponse("Hello", AiDtos.AiProvider.CLAUDE));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void generateContentValidatesTopic() throws Exception {
        mockMvc.perform(post("/api/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void ownerCanGenerateContent() throws Exception {
        when(aiService.generateContent(any()))
                .thenReturn(new AiDtos.ChatResponse("<p>Body</p>", AiDtos.AiProvider.CHATGPT));

        mockMvc.perform(post("/api/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"Summer sale\",\"provider\":\"CHATGPT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("CHATGPT"));
    }
}
