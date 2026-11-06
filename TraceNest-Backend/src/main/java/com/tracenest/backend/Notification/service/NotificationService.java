package com.tracenest.backend.Notification.service;

import com.tracenest.backend.Notification.model.Notification;
import com.tracenest.backend.Notification.repository.NotificationRepository;
import com.tracenest.backend.User.model.User;
import com.tracenest.backend.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public Page<Notification> getNotifications(String userId, boolean unreadOnly, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (unreadOnly) {
            return notificationRepository.findByUserIdAndReadFalse(userId, pageable);
        }
        return notificationRepository.findByUserId(userId, pageable);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(String userId, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new com.tracenest.backend.Common.exception.CustomException(
                        404, "Not Found", "NOT_FOUND", "Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new com.tracenest.backend.Common.exception.CustomException(
                    403, "Forbidden", "NOT_OWNER", "You do not own this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllReadByUserId(userId);
        log.info("Marked all notifications as read for user {}", userId);
    }

    @Transactional
    public void deleteNotificationsByReportId(String reportId) {
        notificationRepository.deleteByReportId(reportId);
        log.info("Deleted notifications for report {}", reportId);
    }

    /**
     * Create an in-app notification for a specific user.
     * Called from SubscriptionService when notifying subscribers who are registered users.
     */
    public Notification createNotification(User user, String type, String title, String body, String reportId) {
        String safeBody = body != null ? body : "";
        if (safeBody.length() > 950) {
            safeBody = safeBody.substring(0, 947) + "...";
        }

        Notification notification = Notification.builder()
                .type(type)
                .title(title)
                .body(safeBody)
                .reportId(reportId)
                .read(false)
                .user(user)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Created in-app notification {} ({}) for user {}", notification.getId(), type, user.getId());
        return notification;
    }
}
