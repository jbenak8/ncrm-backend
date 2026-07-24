package cz.jbenak.ncrm_backend.ai;

import cz.jbenak.ncrm_backend.model.dto.ai.AiDtos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests of {@link AiService} covering chat with conversation history, content generation
 * with default prompt attributes and provider selection (Claude / ChatGPT).
 */
@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private ObjectProvider<AnthropicChatModel> anthropicProvider;
    @Mock
    private ObjectProvider<OpenAiChatModel> openAiProvider;
    @Mock
    private AnthropicChatModel anthropicChatModel;
    @Mock
    private OpenAiChatModel openAiChatModel;

    private AiService service() {
        return new AiService(anthropicProvider, openAiProvider);
    }

    private static ChatResponse response(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    @Test
    void chatUsesClaudeByDefaultAndPassesHistory() {
        when(anthropicProvider.getIfAvailable(any())).thenReturn(anthropicChatModel);
        when(anthropicChatModel.getOptions()).thenReturn(AnthropicChatOptions.builder().build());
        when(anthropicChatModel.call(any(Prompt.class))).thenReturn(response("Hello!"));

        AiDtos.ChatResponse result = service().chat(new AiDtos.ChatRequest("Hi",
                List.of(new AiDtos.ChatMessage("user", "Earlier question"),
                        new AiDtos.ChatMessage("assistant", "Earlier answer")), null));

        assertThat(result.content()).isEqualTo("Hello!");
        assertThat(result.provider()).isEqualTo(AiDtos.AiProvider.CLAUDE);
    }

    @Test
    void chatWithChatGptProvider() {
        when(openAiProvider.getIfAvailable(any())).thenReturn(openAiChatModel);
        when(openAiChatModel.getOptions()).thenReturn(OpenAiChatOptions.builder().build());
        when(openAiChatModel.call(any(Prompt.class))).thenReturn(response("GPT answer"));

        AiDtos.ChatResponse result = service().chat(new AiDtos.ChatRequest("Hi", null, AiDtos.AiProvider.CHATGPT));

        assertThat(result.content()).isEqualTo("GPT answer");
        assertThat(result.provider()).isEqualTo(AiDtos.AiProvider.CHATGPT);
    }

    @Test
    void generateContentFillsDefaultsAndReturnsBody() {
        when(anthropicProvider.getIfAvailable(any())).thenReturn(anthropicChatModel);
        when(anthropicChatModel.getOptions()).thenReturn(AnthropicChatOptions.builder().build());
        when(anthropicChatModel.call(any(Prompt.class))).thenReturn(response("<p>Campaign</p>"));

        AiDtos.ChatResponse result = service().generateContent(
                new AiDtos.ContentGenerationRequest("Summer sale", null, null, null, null));

        assertThat(result.content()).isEqualTo("<p>Campaign</p>");
        assertThat(result.provider()).isEqualTo(AiDtos.AiProvider.CLAUDE);
    }

    @Test
    void chatFailsWhenProviderNotConfigured() {
        when(anthropicProvider.getIfAvailable(any())).thenAnswer(inv -> {
            Supplier<AnthropicChatModel> fallback = inv.getArgument(0);
            return fallback.get();
        });

        AiService service = service();
        AiDtos.ChatRequest request = new AiDtos.ChatRequest("Hi", null, AiDtos.AiProvider.CLAUDE);
        assertThatThrownBy(() -> service.chat(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }
}
