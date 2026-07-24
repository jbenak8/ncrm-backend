package cz.jbenak.ncrm_backend.model.dto.ai;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Container of DTOs used by the AI endpoints (chat and content generation) consumed by the React frontend.
 * Both Claude (Anthropic) and ChatGPT (OpenAI) agents are supported; the provider is selectable per request.
 */
public final class AiDtos {

    private AiDtos() {
    }

    /**
     * Supported AI providers.
     */
    public enum AiProvider {
        CLAUDE, CHATGPT
    }

    /**
     * Request DTO for the conversational chat endpoint. The history allows stateless conversations from the frontend.
     */
    public record ChatRequest(
            @NotBlank String message,
            List<ChatMessage> history,
            AiProvider provider
    ) {
    }

    /**
     * A single message of the conversation history; role is either "user" or "assistant".
     */
    public record ChatMessage(String role, String content) {
    }

    /**
     * Response DTO of the chat and content generation endpoints.
     */
    public record ChatResponse(String content, AiProvider provider) {
    }

    /**
     * Request DTO for AI content generation (e.g. marketing campaign texts, meeting minutes drafts).
     */
    public record ContentGenerationRequest(
            @NotBlank String topic,
            String audience,
            String tone,
            String language,
            AiProvider provider
    ) {
    }
}
