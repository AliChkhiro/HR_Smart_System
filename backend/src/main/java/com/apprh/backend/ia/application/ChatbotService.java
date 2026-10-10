package com.apprh.backend.ia.application;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.ia.api.ChatRequest;
import com.apprh.backend.ia.api.ChatResponse;
import com.apprh.backend.ia.infrastructure.ChatbotClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);
    private final ChatbotClient client;
    private final ChatContextService chatContextService;

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
}