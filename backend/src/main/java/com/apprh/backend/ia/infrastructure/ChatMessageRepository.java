package com.apprh.backend.ia.infrastructure;

import com.apprh.backend.ia.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop40ByUserIdOrderByIdDesc(Long userId);

    List<ChatMessage> findAllByUserIdOrderByIdAsc(Long userId);

    long deleteByUserId(Long userId);
}
