
package com.application.Servease.notification.service;

import com.application.Servease.notification.dto.NotificationResponse;
import com.application.Servease.notification.entity.Notification;
import com.application.Servease.notification.enums.NotificationType;
import com.application.Servease.notification.repository.NotificationRepository;
import com.application.Servease.user.entity.User;
import com.application.Servease.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    @Transactional
    public NotificationResponse createNotification(
            User recipient,
            NotificationType type,
            String title,
            String message,
            String targetUrl,
            String referenceType,
            Long referenceId
    ) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTargetUrl(targetUrl);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setRead(false);

        return NotificationResponse.from(
                notificationRepository.save(notification)
        );
    }

    @Transactional
    public List<NotificationResponse> getMyNotifications(int page, int size) {
        User user = userService.getAuthenticatedUser();

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(
                        user.getId(),
                        PageRequest.of(safePage, safeSize)
                )
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public long getUnreadCount() {
        User user = userService.getAuthenticatedUser();

        return notificationRepository
                .countByRecipientIdAndReadFalse(user.getId());
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        User user = userService.getAuthenticatedUser();

        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification not found."
                ));

        notification.setRead(true);

        return NotificationResponse.from(
                notificationRepository.save(notification)
        );
    }

    @Transactional
    public int markAllAsRead() {
        User user = userService.getAuthenticatedUser();

        return notificationRepository.markAllAsRead(user.getId());
    }
}
