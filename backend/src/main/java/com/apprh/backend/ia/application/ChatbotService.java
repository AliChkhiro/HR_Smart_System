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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private final ChatbotClient client;
    private final ChatContextService chatContextService;
    private final ChatbotQuickAnswerService quickAnswerService;
    private final ObjectMapper objectMapper;

    public ChatResponse ask(UserPrincipal principal, ChatRequest request) {
        log.info("chat - utilisateur {} role {} - message de {} caractères",
                principal.id(), principal.role(), request.message().length());
        Optional<String> quick = quickAnswerService.answer(principal, request.message());
        if (quick.isPresent()) {
            log.info("chat - réponse déterministe pour utilisateur {}", principal.id());
            return new ChatResponse(quick.get(), true);
        }
        try {
            ChatbotClient.ChatContext context = chatContextService.build(principal);
            ChatbotClient.ChatResult r = client.ask(request.message(), history(request), context);
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
        Optional<String> quick = quickAnswerService.answer(principal, request.message());
        if (quick.isPresent()) {
            log.info("chat/stream - réponse déterministe pour utilisateur {}", principal.id());
            writeQuickAnswer(outputStream, quick.get());
            return;
        }
        ChatbotClient.ChatContext context = chatContextService.build(principal);
        try {
            client.stream(request.message(), history(request), context, outputStream);
        } catch (Exception e) {
            log.warn("ia-service/chat stream indisponible: {}", e.getMessage());
            writeFallback(outputStream, principal.firstName());
        }
    }

    private void writeQuickAnswer(OutputStream outputStream, String answer) {
        try {
            outputStream.write((objectMapper.writeValueAsString(new StreamEvent("token", answer)) + "\n")
                    .getBytes(StandardCharsets.UTF_8));
            outputStream.write((objectMapper.writeValueAsString(new StreamEvent("done", null)) + "\n")
                    .getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException ignored) {
            log.debug("flux interrompu, réponse déterministe non transmise");
        }
    }

    private List<ChatbotClient.History> history(ChatRequest request) {
        if (request.history() == null) {
            return List.of();
        }
        return request.history().stream()
                .map(h -> new ChatbotClient.History(h.role(), h.content()))
                .toList();
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
