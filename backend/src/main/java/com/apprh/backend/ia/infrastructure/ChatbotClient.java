package com.apprh.backend.ia.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ChatbotClient {

    private static final Logger log = LoggerFactory.getLogger(ChatbotClient.class);
    private final RestClient restClient;

    public ChatbotClient(ChatbotProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Math.min(props.timeoutMs(), 30000L));
        factory.setReadTimeout((int) Math.min(props.timeoutMs(), 60000L));
        this.restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();
    }

    public ChatResult ask(String message, ChatContext context) {
        ChatReq req = new ChatReq(message, context);
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

    private record ChatReq(String message, ChatContext user_context) {
    }

    private record ChatRes(String reply, boolean ai_available) {
    }
}
