package com.apprh.backend.ia.application;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.ia.api.ChatRequest;
import com.apprh.backend.ia.api.ChatResponse;
import com.apprh.backend.ia.infrastructure.ChatbotClient;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private final ChatbotClient client;
    private final ChatContextService chatContextService;
    private final ObjectMapper objectMapper;

    public ChatResponse ask(UserPrincipal principal, ChatRequest request) {
        log.info("chat - utilisateur {} role {} - message de {} caractères",
                principal.id(), principal.role(), request.message().length());
        try {
            ChatbotClient.ChatContext context = chatContextService.build(principal);
            ChatbotClient.ChatResult r = client.ask(request.message(), context);
            return new ChatResponse(r.reply(), r.aiAvailable());
        } catch (Exception e) {
            log.warn("ia-service/chat indisponible: {}", e.getMessage());
            return new ChatResponse(
                    "Bonjour " + principal.firstName() + ", l'IA est momentanément indisponible. Votre message est bien reçu.",
                    false
            );
        }
    }

    /**
     * Version en flux : le contexte est construit sur le thread de requête (transaction),
     * puis la réponse NDJSON de l'ia-service est relayée telle quelle au client.
     */
    public void stream(UserPrincipal principal, ChatRequest request, OutputStream outputStream) {
        log.info("chat/stream - utilisateur {} role {} - message de {} caractères",
                principal.id(), principal.role(), request.message().length());
        ChatbotClient.ChatContext context = chatContextService.build(principal);
        try {
            client.stream(request.message(), context, outputStream);
        } catch (Exception e) {
            log.warn("ia-service/chat stream indisponible: {}", e.getMessage());
            writeFallback(outputStream, principal.firstName());
        }
    }

    private void writeFallback(OutputStream outputStream, String firstName) {
        try {
            String line = objectMapper.writeValueAsString(new StreamEvent("fallback",
                    "Bonjour " + firstName + ", l'IA est momentanément indisponible. Votre message est bien reçu.")) + "\n";
            outputStream.write(line.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException ignored) {
            log.debug("flux interrompu, repli non transmis");
        }
    }

    private record StreamEvent(String type, String text) {
    }
}
