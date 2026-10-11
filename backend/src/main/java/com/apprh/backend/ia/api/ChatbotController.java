package com.apprh.backend.ia.api;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.ia.application.ChatHistoryService;
import com.apprh.backend.ia.application.ChatbotService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ChatHistoryService chatHistoryService;

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

    @GetMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    public List<ChatMessageDto> messages(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return chatHistoryService.recent(principal.id()).stream()
                .map(message -> new ChatMessageDto(message.getRole(), message.getContent()))
                .toList();
    }

    @DeleteMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearMessages(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        chatHistoryService.clear(principal.id());
    }
}
