package com.apprh.backend.ia.application;

import com.apprh.backend.ia.domain.ChatMessage;
import com.apprh.backend.ia.infrastructure.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistance (côté serveur) des échanges du chatbot, scoping strictement
 * par identifiant utilisateur issu du JWT.
 */
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private static final int MAX_CONTENT_LENGTH = 4000;

    private final ChatMessageRepository repository;

    @Transactional
    public void recordExchange(Long userId, String userMessage, String assistantMessage) {
        save(userId, "user", userMessage);
        save(userId, "assistant", assistantMessage);
    }

    private void save(Long userId, String role, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        String value = content.length() > MAX_CONTENT_LENGTH ? content.substring(0, MAX_CONTENT_LENGTH) : content;
        repository.save(ChatMessage.builder().userId(userId).role(role).content(value).build());
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> recent(Long userId) {
        List<ChatMessage> messages = new ArrayList<>(repository.findTop40ByUserIdOrderByIdDesc(userId));
        Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public void clear(Long userId) {
        repository.deleteByUserId(userId);
    }
}
