package com.softcrm.entity;

import com.softcrm.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_is_read", columnList = "is_read"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String message;

    @Column(name = "notification_type")
    private String type; // INFO, SUCCESS, WARNING, ERROR, ALERT, REMINDER

    @Column(name = "category")
    private String category; // LEAD, DEAL, TASK, TICKET, MEETING, SYSTEM

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "entity_type")
    private String entityType; // LEAD, DEAL, CUSTOMER, TASK, TICKET, MEETING

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "icon")
    private String icon;

    @Column(name = "background_color")
    private String backgroundColor;

    @Column(name = "priority")
    private String priority; // LOW, MEDIUM, HIGH, URGENT

    @Builder.Default
    @Column(name = "is_read")
    private Boolean isRead = false;

    @Builder.Default
    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Metadata as JSON string
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // ================= Helper Methods =================

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public void markAsUnread() {
        this.isRead = false;
        this.readAt = null;
    }

    public void archive() {
        this.isArchived = true;
    }

    public void restore() {
        this.isArchived = false;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isUrgent() {
        return "URGENT".equals(priority) || "HIGH".equals(priority);
    }

    // ================= Builder Helper Methods =================

    public static Notification.NotificationBuilder create(
            String title,
            String message,
            User user
    ) {
        return Notification.builder()
                .title(title)
                .message(message)
                .user(user)
                .type("INFO")
                .isRead(false)
                .isArchived(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now());
    }

    public static Notification success(
            String title,
            String message,
            User user
    ) {
        return Notification.builder()
                .title(title)
                .message(message)
                .user(user)
                .type("SUCCESS")
                .icon("fa-check-circle")
                .backgroundColor("#48bb78")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Notification warning(
            String title,
            String message,
            User user
    ) {
        return Notification.builder()
                .title(title)
                .message(message)
                .user(user)
                .type("WARNING")
                .icon("fa-exclamation-triangle")
                .backgroundColor("#ed8936")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Notification error(
            String title,
            String message,
            User user
    ) {
        return Notification.builder()
                .title(title)
                .message(message)
                .user(user)
                .type("ERROR")
                .icon("fa-times-circle")
                .backgroundColor("#f56565")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Notification info(
            String title,
            String message,
            User user
    ) {
        return Notification.builder()
                .title(title)
                .message(message)
                .user(user)
                .type("INFO")
                .icon("fa-info-circle")
                .backgroundColor("#4299e1")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Notification reminder(
            String title,
            String message,
            User user,
            LocalDateTime expiresAt
    ) {
        return Notification.builder()
                .title(title)
                .message(message)
                .user(user)
                .type("REMINDER")
                .priority("MEDIUM")
                .icon("fa-bell")
                .backgroundColor("#ed8936")
                .expiresAt(expiresAt)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Notification alert(
            String title,
            String message,
            User user,
            String priority
    ) {
        return Notification.builder()
                .title(title)
                .message(message)
                .user(user)
                .type("ALERT")
                .priority(priority)
                .icon("fa-bell")
                .backgroundColor("#f56565")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}