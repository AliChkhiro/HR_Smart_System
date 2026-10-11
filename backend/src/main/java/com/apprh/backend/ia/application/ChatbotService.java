package com.apprh.backend.ia.application;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.ia.api.ChatRequest;
import com.apprh.backend.ia.api.ChatResponse;
import com.apprh.backend.ia.infrastructure.ChatbotClient;
import com.apprh.backend.ia.infrastructure.ChatbotMetrics;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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
    private final ChatHistoryService chatHistoryService;
    private final ChatbotMetrics metrics;
    private final ObjectMapper objectMapper;

    public ChatResponse ask(UserPrincipal principal, ChatRequest request) {
        log.info("chat - utilisateur {} role {} - message de {} caractères",
                principal.id(), principal.role(), request.message().length());
        Optional<String> quick = quickAnswerService.answer(principal, request.message());
        if (quick.isPresent()) {
            log.info("chat - réponse déterministe pour utilisateur {}", principal.id());
            metrics.recordRequest("chat", "deterministic");
            persist(principal.id(), request.message(), quick.get());
            return new ChatResponse(quick.get(), true);
        }
        long start = System.nanoTime();
        try {
            ChatbotClient.ChatContext context = chatContextService.build(principal);
            ChatbotClient.ChatResult r = client.ask(request.message(), history(request), context);
            metrics.recordRequest("chat", "llm");
            metrics.recordLatency("chat", System.nanoTime() - start);
            persist(principal.id(), request.message(), r.reply());
            return new ChatResponse(r.reply(), r.aiAvailable(), r.sources());
        } catch (Exception e) {
            metrics.recordRequest("chat", "fallback");
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
     * Le texte assemblé est capturé pour être persisté côté serveur.
     */
    public void stream(UserPrincipal principal, ChatRequest request, OutputStream outputStream) {
        log.info("chat/stream - utilisateur {} role {} - message de {} caractères",
                principal.id(), principal.role(), request.message().length());
        Optional<String> quick = quickAnswerService.answer(principal, request.message());
        if (quick.isPresent()) {
            log.info("chat/stream - réponse déterministe pour utilisateur {}", principal.id());
            metrics.recordRequest("stream", "deterministic");
            writeQuickAnswer(outputStream, quick.get());
            persist(principal.id(), request.message(), quick.get());
            return;
        }
        ChatbotClient.ChatContext context = chatContextService.build(principal);
        NdjsonCapture capture = new NdjsonCapture(outputStream);
        long start = System.nanoTime();
        try {
            client.stream(request.message(), history(request), context, capture);
            metrics.recordRequest("stream", "llm");
            metrics.recordLatency("stream", System.nanoTime() - start);
            String assembled = capture.assemble(objectMapper);
            if (!assembled.isBlank()) {
                persist(principal.id(), request.message(), assembled);
            }
        } catch (Exception e) {
            metrics.recordRequest("stream", "fallback");
            log.warn("ia-service/chat stream indisponible: {}", e.getMessage());
            writeFallback(outputStream, principal.firstName());
        }
    }

    /**
     * Persiste l'échange sans jamais compromettre la réponse déjà produite :
     * une défaillance d'écriture est journalisée mais n'entraîne pas de repli.
     */
    private void persist(Long userId, String userMessage, String assistantMessage) {
        try {
            chatHistoryService.recordExchange(userId, userMessage, assistantMessage);
        } catch (Exception e) {
            log.warn("historique non persisté pour utilisateur {}: {}", userId, e.getMessage());
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

    private void writeFallback(OutputStream outputStream, String firstName) {
        try {
            String line = objectMapper.writeValueAsString(new StreamEvent("fallback",
                    "Bonjour " + firstName + ", l'IA est momentanément indisponible. Votre message est bien reçu."));
            outputStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException ignored) {
            log.debug("flux interrompu, repli non transmis");
        }
    }

    private record StreamEvent(String type, String text) {
    }

    /**
     * Flux de sortie qui relaie les octets vers le client tout en accumulant la
     * réponse NDJSON, afin d'en reconstituer le texte final pour la persistance.
     * Le décodage se fait sur l'intégralité du flux (pas de coupure multi-octets).
     */
    private static final class NdjsonCapture extends OutputStream {

        private final OutputStream delegate;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        private NdjsonCapture(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
            buffer.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            buffer.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        private String assemble(ObjectMapper mapper) {
            String text = buffer.toString(StandardCharsets.UTF_8);
            StringBuilder assembled = new StringBuilder();
            for (String line : text.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    JsonNode node = mapper.readTree(trimmed);
                    String type = node.path("type").asText();
                    if (("token".equals(type) || "fallback".equals(type)) && node.hasNonNull("text")) {
                        assembled.append(node.get("text").asText());
                    }
                } catch (Exception ignored) {
                    // fragment non JSON ignoré
                }
            }
            return assembled.toString();
        }
    }
}
