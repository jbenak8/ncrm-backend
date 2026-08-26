package cz.jbenak.ncrm_backend.ai;

import cz.jbenak.ncrm_backend.model.dto.ai.AiDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 *
 * Service integrating the AI agents (Claude / Anthropic and ChatGPT / OpenAI) via Spring AI.
 * Provides conversational chat and content generation (e.g. marketing campaign texts) for the frontend.
 * Claude is the default provider; the provider is selectable per request. The agents have read-only
 * access to real CRM data (store catalogue, item categories and orders) via {@link CrmAiTools}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final AiDtos.AiProvider DEFAULT_PROVIDER = AiDtos.AiProvider.CLAUDE;

    private final ObjectProvider<AnthropicChatModel> anthropicChatModel;
    private final ObjectProvider<OpenAiChatModel> openAiChatModel;
    private final CrmAiTools crmAiTools;

    /**
     * Conversational chat with the selected AI agent. The conversation history is passed
     * from the frontend, so the endpoint stays stateless.
     */
    public AiDtos.ChatResponse chat(AiDtos.ChatRequest request) {
        AiDtos.AiProvider provider = request.provider() == null ? DEFAULT_PROVIDER : request.provider();
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("""
                You are a helpful assistant integrated in the nCRM application. Answer briefly and to the point.
                You have tools to look up real CRM data: the store catalogue (items with their current prices),
                the item category tree and customer orders. Use them whenever the user asks about products,
                prices, item groups or orders instead of guessing."""));
        if (request.history() != null) {
            for (AiDtos.ChatMessage history : request.history()) {
                messages.add("assistant".equalsIgnoreCase(history.role())
                        ? new AssistantMessage(history.content())
                        : new UserMessage(history.content()));
            }
        }
        messages.add(new UserMessage(request.message()));
        log.info("AI chat request via {} with {} history message(s)", provider,
                request.history() == null ? 0 : request.history().size());
        String content = chatClient(provider).prompt().messages(messages).tools(crmAiTools).call().content();
        return new AiDtos.ChatResponse(content, provider);
    }

    /**
     * Generates marketing or other content (e.g. campaign e-mail body) with the selected AI agent.
     */
    public AiDtos.ChatResponse generateContent(AiDtos.ContentGenerationRequest request) {
        AiDtos.AiProvider provider = request.provider() == null ? DEFAULT_PROVIDER : request.provider();
        String prompt = """
                Write a marketing e-mail for the following campaign.
                Topic: %s
                Target audience: %s
                Tone: %s
                Language: %s
                You have tools to look up real CRM data: the store catalogue (items with their current prices),
                the item category tree and customer orders. Use them to base the campaign on real products,
                real names and real prices. When the topic asks for a discount (e.g. "5%% cheaper"),
                compute the discounted price from the item's current price and state both prices.
                Do not invent products or prices that are not in the catalogue.
                Return only the e-mail body as HTML without any explanation.
                """.formatted(request.topic(),
                request.audience() == null ? "existing B2B customers" : request.audience(),
                request.tone() == null ? "professional and friendly" : request.tone(),
                request.language() == null ? "Czech" : request.language());
        log.info("AI content generation via {} for topic '{}'", provider, request.topic());
        String content = chatClient(provider).prompt().user(prompt).tools(crmAiTools).call().content();
        return new AiDtos.ChatResponse(content, provider);
    }

    private ChatClient chatClient(AiDtos.AiProvider provider) {
        return switch (provider) {
            case CLAUDE -> ChatClient.create(anthropicChatModel.getIfAvailable(
                    () -> { throw new IllegalStateException("Claude (Anthropic) model is not configured"); }));
            case CHATGPT -> ChatClient.create(openAiChatModel.getIfAvailable(
                    () -> { throw new IllegalStateException("ChatGPT (OpenAI) model is not configured"); }));
        };
    }
}
