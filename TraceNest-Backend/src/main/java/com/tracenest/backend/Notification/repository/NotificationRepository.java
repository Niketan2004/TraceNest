package com.tracenest.backend.Notification.repository;

import com.tracenest.backend.Notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<Notification> findByUserId(String userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadFalse(String userId, Pageable pageable);

    long countByUserIdAndReadFalse(String userId);

    void deleteByReportId(String reportId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllReadByUserId(@Param("userId") String userId);
}
