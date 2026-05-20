package ch.uzh.ifi.hase.soprafs26.service.bot;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

class HuggingFaceBotAiClientTest {

    @Test
    void constructor_normalizesTokenModelAndTimeout() throws Exception {
        HuggingFaceBotAiClient client = new HuggingFaceBotAiClient(true, "  token  ", "   ", 100L, new ObjectMapper());

        assertEquals("token", readField(client, "apiToken"));
        assertEquals("openai/gpt-oss-120b:fastest", readField(client, "model"));
        assertEquals("PT0.5S", readField(client, "timeout").toString());

        HuggingFaceBotAiClient client2 = new HuggingFaceBotAiClient(true, null, "my/model", 2500L, new ObjectMapper());
        assertEquals("", readField(client2, "apiToken"));
        assertEquals("my/model", readField(client2, "model"));
        assertEquals("PT2.5S", readField(client2, "timeout").toString());
    }

    @Test
    void generateDecision_returnsEmptyWhenDisabledOrBlankInput() {
        HuggingFaceBotAiClient disabled = new HuggingFaceBotAiClient(false, "token", "model", 2000L, new ObjectMapper());
        HuggingFaceBotAiClient blankToken = new HuggingFaceBotAiClient(true, " ", "model", 2000L, new ObjectMapper());
        HuggingFaceBotAiClient active = new HuggingFaceBotAiClient(true, "token", "model", 2000L, new ObjectMapper());

        assertTrue(disabled.generateDecision("{}").isEmpty());
        assertTrue(blankToken.generateDecision("{}").isEmpty());
        assertTrue(active.generateDecision(" ").isEmpty());
        assertTrue(active.generateDecision(null).isEmpty());
    }

    @Test
    void generateDecision_non2xxResponse_returnsEmpty() throws Exception {
        HuggingFaceBotAiClient client = new HuggingFaceBotAiClient(true, "token", "model", 2000L, new ObjectMapper());
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(503);
        when(response.body()).thenReturn("{\"choices\":[]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        setHttpClient(client, httpClient);

        Optional<String> result = client.generateDecision("{\"A\":[]}");

        assertTrue(result.isEmpty());
    }

    @Test
    void generateDecision_validMessageContent_returnsContent() throws Exception {
        HuggingFaceBotAiClient client = new HuggingFaceBotAiClient(true, "token", "model", 2000L, new ObjectMapper());
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"{\\\"chosenActionId\\\":\\\"A2\\\"}\"}}]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        setHttpClient(client, httpClient);

        Optional<String> result = client.generateDecision("{\"A\":[]}");

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("chosenActionId"));
    }

    @Test
    void generateDecision_whenInterrupted_returnsEmptyAndPreservesInterruptFlag() throws Exception {
        HuggingFaceBotAiClient client = new HuggingFaceBotAiClient(true, "token", "model", 2000L, new ObjectMapper());
        HttpClient httpClient = mock(HttpClient.class);

        doThrow(new InterruptedException("stop")).when(httpClient)
            .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        setHttpClient(client, httpClient);

        Optional<String> result = client.generateDecision("{\"A\":[]}");

        assertTrue(result.isEmpty());
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
    }

    @Test
    void generateDecision_whenIoOrRuntimeException_returnsEmpty() throws Exception {
        HuggingFaceBotAiClient client = new HuggingFaceBotAiClient(true, "token", "model", 2000L, new ObjectMapper());
        HttpClient httpClient = mock(HttpClient.class);

        doThrow(new IOException("network")).when(httpClient)
            .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        setHttpClient(client, httpClient);
        assertTrue(client.generateDecision("{\"A\":[]}").isEmpty());

        HttpClient httpClient2 = mock(HttpClient.class);
        doThrow(new RuntimeException("boom")).when(httpClient2)
            .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        setHttpClient(client, httpClient2);
        assertTrue(client.generateDecision("{\"A\":[]}").isEmpty());
    }

    @Test
    void extractMessageContent_coversEdgeCases() {
        HuggingFaceBotAiClient client = new HuggingFaceBotAiClient(true, "token", "model", 2000L, new ObjectMapper());

        assertTrue(invokeExtract(client, null).isEmpty());
        assertTrue(invokeExtract(client, " ").isEmpty());
        assertTrue(invokeExtract(client, "{}").isEmpty());
        assertTrue(invokeExtract(client, "{\"choices\":[]}").isEmpty());
        assertTrue(invokeExtract(client, "{\"choices\":[\"x\"]}").isEmpty());
        assertTrue(invokeExtract(client, "{\"choices\":[{\"message\":{\"content\":\"\"}}]}").isEmpty());
        assertTrue(invokeExtract(client, "{\"choices\":[{\"message\":{\"content\":123}}]}").isEmpty());
        assertTrue(invokeExtract(client, "{\"choices\":[{\"text\":\"\"}]}").isEmpty());

        Optional<String> fromText = invokeExtract(client, "{\"choices\":[{\"text\":\"  choose A1  \"}]}");
        assertTrue(fromText.isPresent());

        Optional<String> fromMessage = invokeExtract(client, "{\"choices\":[{\"message\":{\"content\":\"{\\\"chosenActionId\\\":\\\"A1\\\"}\"}}]}");
        assertTrue(fromMessage.isPresent());

        assertFalse(invokeExtract(client, "not-json").isPresent());
    }

    private void setHttpClient(HuggingFaceBotAiClient client, HttpClient mockClient) throws Exception {
        Field field = HuggingFaceBotAiClient.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(client, mockClient);
    }

    private Object readField(HuggingFaceBotAiClient client, String fieldName) throws Exception {
        Field field = HuggingFaceBotAiClient.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(client);
    }

    @SuppressWarnings("unchecked")
    private Optional<String> invokeExtract(HuggingFaceBotAiClient client, String body) {
        try {
            Method method = HuggingFaceBotAiClient.class.getDeclaredMethod("extractMessageContent", String.class);
            method.setAccessible(true);
            return (Optional<String>) method.invoke(client, body);
        } catch (Exception exception) {
            throw new AssertionError("Failed to invoke extractMessageContent", exception);
        }
    }
}
