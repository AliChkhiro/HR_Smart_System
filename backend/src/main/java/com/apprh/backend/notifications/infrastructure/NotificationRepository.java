package com.apprh.backend.notifications.infrastructure;

import com.apprh.backend.notifications.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadFalseAndDeletedAtIsNull(Long userId);

    Optional<Notification> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    @Modifying
    @Query("update Notification n set n.read = true where n.userId = :userId and n.deletedAt is null")
    void markAllReadForUser(Long userId);
}
