package com.apprh.backend.ia.api;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.ia.application.ChatbotService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return chatbotService.ask(principal, request);
    }

    /**
     * Relaie le flux NDJSON de l'ia-service en écriture directe sur la réponse
     * (le StreamingResponseBody de Spring bufferiserait tout jusqu'à la fin).
     */
    @PostMapping(value = "/chat/stream", produces = "application/x-ndjson")
    @PreAuthorize("isAuthenticated()")
    public void chatStream(@Valid @RequestBody ChatRequest request, Authentication authentication,
                           HttpServletResponse response) throws IOException {
        response.setContentType("application/x-ndjson");
        response.setCharacterEncoding("UTF-8");
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        chatbotService.stream(principal, request, response.getOutputStream());
    }
}
