package org.nagrivic.modules.notifications.controller;

import jakarta.validation.Valid;
import org.nagrivic.common.dto.PagedResponse;
import org.nagrivic.modules.notifications.dto.NotificationPreferenceResponse;
import org.nagrivic.modules.notifications.dto.NotificationResponse;
import org.nagrivic.modules.notifications.dto.UnreadCountResponse;
import org.nagrivic.modules.notifications.dto.UpdateNotificationPreferenceRequest;
import org.nagrivic.modules.notifications.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size
    ) {
        int clampedPage = Math.max(page, 0);
        int clampedSize = Math.min(Math.max(size, 1), MAX_SIZE);
        Pageable pageable = PageRequest.of(clampedPage, clampedSize);

        Page<NotificationResponse> pageResult = notificationService.getNotificationsForCurrentUser(pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(pageResult));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        UnreadCountResponse response = notificationService.getUnreadCountForCurrentUser();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID notificationId) {
        NotificationResponse response = notificationService.markAsReadForCurrentUser(notificationId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        notificationService.markAllAsReadForCurrentUser();
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences() {
        NotificationPreferenceResponse response = notificationService.getPreferencesForCurrentUser();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            @Valid @RequestBody UpdateNotificationPreferenceRequest request
    ) {
        NotificationPreferenceResponse response = notificationService.updatePreferencesForCurrentUser(request);
        return ResponseEntity.ok(response);
    }
}
