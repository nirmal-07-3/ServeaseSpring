
package com.application.Servease.notification.controller;

import com.application.Servease.notification.dto.NotificationResponse;
import com.application.Servease.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationService.getMyNotifications(page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        return Map.of(
                "unreadCount",
                notificationService.getUnreadCount()
        );
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllAsRead() {
        return Map.of(
                "updatedCount",
                notificationService.markAllAsRead()
        );
    }
}
