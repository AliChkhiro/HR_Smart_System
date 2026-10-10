package com.apprh.backend;

import com.apprh.backend.ia.application.ChatHistoryService;
import com.apprh.backend.ia.domain.ChatMessage;
import com.apprh.backend.ia.infrastructure.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceTest {

    @Mock
    private ChatMessageRepository repository;

    @InjectMocks
    private ChatHistoryService service;

    @Test
    void truncatesContentToColumnLimit() {
        String longMessage = "x".repeat(5000);

        service.recordExchange(1L, longMessage, null);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getContent()).hasSize(4000);
        assertThat(captor.getValue().getRole()).isEqualTo("user");
    }

    @Test
    void skipsBlankMessages() {
        service.recordExchange(1L, "   ", null);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(ChatMessage.class));
    }

    @Test
    void storesBothRoles() {
        service.recordExchange(1L, "question", "réponse");
        verify(repository, org.mockito.Mockito.times(2))
                .save(org.mockito.ArgumentMatchers.any(ChatMessage.class));
    }

    @Test
    void recentReturnsChronologicalOrder() {
        ChatMessage newest = ChatMessage.builder().id(2L).userId(1L).role("assistant").content("b").build();
        ChatMessage oldest = ChatMessage.builder().id(1L).userId(1L).role("user").content("a").build();
        when(repository.findTop40ByUserIdOrderByIdDesc(1L)).thenReturn(List.of(newest, oldest));

        List<ChatMessage> result = service.recent(1L);

        assertThat(result).extracting(ChatMessage::getId).containsExactly(1L, 2L);
    }

    @Test
    void clearDelegatesToRepository() {
        service.clear(7L);
        verify(repository).deleteByUserId(7L);
    }
}
