package com.tracenest.backend.User.model;

import com.tracenest.backend.Notification.model.Notification;
import com.tracenest.backend.Report.model.Report;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // A simple relation for refreshToken (if we don't want a separate table for now)
    @Column(name = "refresh_token", length = 512)
    private String refreshToken;

    @Column(name = "refresh_token_expiry")
    private OffsetDateTime refreshTokenExpiry;

    @Column(name = "reset_token", length = 512)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private OffsetDateTime resetTokenExpiry;

    @OneToMany(mappedBy = "reporter", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Report> reports;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Notification> notifications;

    public User() {
    }

    public User(String id, String name, String email, String password, String phone, String city, Role role, String avatarUrl, OffsetDateTime createdAt, String refreshToken, OffsetDateTime refreshTokenExpiry, String resetToken, OffsetDateTime resetTokenExpiry, java.util.List<Report> reports, java.util.List<Notification> notifications) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.city = city;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.refreshToken = refreshToken;
        this.refreshTokenExpiry = refreshTokenExpiry;
        this.resetToken = resetToken;
        this.resetTokenExpiry = resetTokenExpiry;
        this.reports = reports;
        this.notifications = notifications;
    }
}
