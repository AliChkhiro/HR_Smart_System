package com.apprh.backend.notifications.application;

import com.apprh.backend.common.exception.ApiException;
import com.apprh.backend.notifications.domain.Notification;
import com.apprh.backend.notifications.domain.NotificationType;
import com.apprh.backend.notifications.infrastructure.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification create(Long userId, NotificationType type, String message) {
        return notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalseAndDeletedAtIsNull(userId);
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        Notification notification = notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND",
                        "Notification introuvable"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadForUser(userId);
    }
}
