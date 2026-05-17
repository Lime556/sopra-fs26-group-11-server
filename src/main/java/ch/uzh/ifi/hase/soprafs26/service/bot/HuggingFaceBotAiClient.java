package ch.uzh.ifi.hase.soprafs26.service.bot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class HuggingFaceBotAiClient implements BotAiClient {

    private static final URI HF_CHAT_COMPLETIONS_URI = URI.create("https://router.huggingface.co/v1/chat/completions");

    private final boolean enabled;
    private final String apiToken;
    private final String model;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HuggingFaceBotAiClient(
            @Value("${bot.ai.enabled:false}") boolean enabled,
            @Value("${bot.ai.api-token:}") String apiToken,
            @Value("${bot.ai.model:openai/gpt-oss-120b:fastest}") String model,
            @Value("${bot.ai.timeout-ms:4000}") long timeoutMs,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.apiToken = apiToken == null ? "" : apiToken.trim();
        this.model = model == null || model.isBlank() ? "openai/gpt-oss-120b:fastest" : model.trim();
        this.timeout = Duration.ofMillis(Math.max(500L, timeoutMs));
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(this.timeout)
            .build();
    }

    @Override
    public Optional<String> generateDecision(String prompt) {
        if (!enabled || apiToken.isBlank() || prompt == null || prompt.isBlank()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> systemMessage = new LinkedHashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", BotAiService.SYSTEM_PROMPT);

            Map<String, Object> userMessage = new LinkedHashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            Map<String, Object> requestPayload = new LinkedHashMap<>();
            requestPayload.put("model", model);
            requestPayload.put("messages", List.of(systemMessage, userMessage));
            requestPayload.put("stream", false);
            requestPayload.put("temperature", 0.2);
            requestPayload.put("max_tokens", 256);

            HttpRequest request = HttpRequest.newBuilder(HF_CHAT_COMPLETIONS_URI)
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestPayload), StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            return extractMessageContent(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<String> extractMessageContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return Optional.empty();
        }

        try {
            Map<?, ?> response = objectMapper.readValue(responseBody, Map.class);
            Object choices = response.get("choices");
            if (!(choices instanceof List<?> choiceList) || choiceList.isEmpty()) {
                return Optional.empty();
            }

            Object firstChoice = choiceList.get(0);
            if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
                return Optional.empty();
            }

            Object message = choiceMap.get("message");
            if (message instanceof Map<?, ?> messageMap) {
                Object content = messageMap.get("content");
                if (content instanceof String contentString && !contentString.isBlank()) {
                    return Optional.of(contentString);
                }
            }

            Object text = choiceMap.get("text");
            if (text instanceof String textString && !textString.isBlank()) {
                return Optional.of(textString);
            }

            return Optional.empty();
        } catch (IOException exception) {
            return Optional.empty();
        }
    }
}