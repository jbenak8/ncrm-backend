package cz.jbenak.ncrm_backend.controler;

import cz.jbenak.ncrm_backend.ai.AiService;
import cz.jbenak.ncrm_backend.model.dto.ai.AiDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * REST API for active AI usage in the frontend: conversational chat and content generation.
 * Both Claude and ChatGPT agents are supported; available to internal roles only.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'SALES_REPRESENTATIVE')")
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public AiDtos.ChatResponse chat(@Valid @RequestBody AiDtos.ChatRequest request) {
        return aiService.chat(request);
    }

    @PostMapping("/generate")
    public AiDtos.ChatResponse generateContent(@Valid @RequestBody AiDtos.ContentGenerationRequest request) {
        return aiService.generateContent(request);
    }
}
