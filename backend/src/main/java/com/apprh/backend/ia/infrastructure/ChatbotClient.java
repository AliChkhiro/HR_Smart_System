package com.apprh.backend.ia.infrastructure;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
public class ChatbotClient {

    private static final Logger log = LoggerFactory.getLogger(ChatbotClient.class);
    private static final int STREAM_BUFFER_SIZE = 8192;

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final long readTimeoutMs;

    public ChatbotClient(ChatbotProperties props, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.baseUrl = props.baseUrl();
        this.readTimeoutMs = props.timeoutMs();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Math.min(props.timeoutMs(), 30000L));
        factory.setReadTimeout((int) Math.min(props.timeoutMs(), 60000L));
        this.restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(Math.min(props.timeoutMs(), 30000L)))
                .build();
    }

    public ChatResult ask(String message, List<History> history, ChatContext context) {
        ChatReq req = new ChatReq(message, context, history);
        try {
            ChatRes res = restClient.post()
                    .uri("/api/v1/chat")
                    .body(req)
                    .retrieve()
                    .body(ChatRes.class);
            if (res == null) {
                return new ChatResult("L'IA est indisponible.", false);
            }
            return new ChatResult(res.reply(), res.ai_available());
        } catch (Exception e) {
            log.warn("ia-service/chat indisponible: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Relaie le flux NDJSON de l'ia-service vers le flux de sortie HTTP,
     * en propageant chaque fragment dès sa réception.
     */
    public void stream(String message, List<History> history, ChatContext context, OutputStream output)
            throws IOException, InterruptedException {
        String payload = objectMapper.writeValueAsString(new ChatReq(message, context, history));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/chat/stream"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(Math.min(readTimeoutMs, 30000L)))
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String detail;
            try (InputStream body = response.body()) {
                detail = new String(body.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new IOException("ia-service stream HTTP " + response.statusCode() + " body=" + detail);
        }
        try (InputStream body = response.body()) {
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            int read;
            while ((read = body.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                output.flush();
            }
        }
    }

    public record ChatResult(String reply, boolean aiAvailable) {
    }

    public record ChatContext(
            String firstName,
            String lastName,
            String role,
            String jobTitle,
            String department,
            String hireDate,
            String leaveSummary,
            List<String> recentLeaves,
            List<String> inProgressTasks,
            List<String> projects
    ) {
    }

    public record History(String role, String content) {
    }

    private record ChatReq(String message, ChatContext user_context, List<History> history) {
    }

    private record ChatRes(String reply, boolean ai_available) {
    }
}
