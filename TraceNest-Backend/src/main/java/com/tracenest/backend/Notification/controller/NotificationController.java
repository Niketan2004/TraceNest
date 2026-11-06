package com.tracenest.backend.Notification.controller;

import com.tracenest.backend.Auth.security.UserDetailsImpl;
import com.tracenest.backend.Notification.model.Notification;
import com.tracenest.backend.Notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller providing endpoints for in-app alert queries, unread badge counters,
 * and read-status mutations for authenticated users.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Fetches a paginated slice of in-app notifications for the authenticated user.
     *
     * @param userDetails Authenticated user principal.
     * @param unreadOnly Filter only unread notifications if true.
     * @param page Zero-indexed page.
     * @param size Number of items per page.
     * @return {@link ResponseEntity} containing notification list and unread count metadata.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Notification> notifPage = notificationService.getNotifications(
                userDetails.getId(), unreadOnly, page, size);
        long unreadCount = notificationService.getUnreadCount(userDetails.getId());

        // Map notifications to DTOs (avoid sending full User object)
        List<Map<String, Object>> items = notifPage.getContent().stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("type", n.getType());
            map.put("title", n.getTitle());
            map.put("body", n.getBody());
            map.put("reportId", n.getReportId());
            map.put("read", n.isRead());
            map.put("createdAt", n.getCreatedAt());
            return map;
        }).toList();

        Map<String, Object> response = Map.of(
                "items", items,
                "unreadCount", unreadCount,
                "page", notifPage.getNumber(),
                "size", notifPage.getSize(),
                "totalItems", notifPage.getTotalElements(),
                "totalPages", notifPage.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Marks a specific notification as read, enforcing user ownership.
     *
     * @param userDetails Authenticated user principal.
     * @param id Notification primary key.
     * @return {@link ResponseEntity} with HTTP 204 No Content.
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String id) {
        notificationService.markAsRead(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marks all unread notifications for the authenticated user as read in a single batch.
     *
     * @param userDetails Authenticated user principal.
     * @return {@link ResponseEntity} with HTTP 204 No Content.
     */
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
