package com.softcrm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Recent Activity Data Transfer Object
 * Used for activity feed and timeline display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDTO {

    private Long id;

    // LEAD, CUSTOMER, DEAL, TASK, TICKET
    private String type;

    // CREATED, UPDATED, CONVERTED, COMPLETED, RESOLVED, ASSIGNED
    private String action;

    private String title;

    private String description;

    private String userName;

    private Long userId;

    private String userAvatar;

    private LocalDateTime activityTime;

    private String entityId;

    private String entityName;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    // ================= COMPUTED METHODS =================

    public String getIcon() {
        switch (type != null ? type : "") {
            case "LEAD":
                return "fa-user-plus";
            case "CUSTOMER":
                return "fa-user-check";
            case "DEAL":
                return "fa-handshake";
            case "TASK":
                return "fa-tasks";
            case "TICKET":
                return "fa-ticket-alt";
            default:
                return "fa-bell";
        }
    }

    public String getColor() {
        switch (action != null ? action : "") {
            case "CREATED":
                return "#48bb78";
            case "UPDATED":
                return "#3498db";
            case "CONVERTED":
                return "#9b59b6";
            case "COMPLETED":
                return "#2ecc71";
            case "RESOLVED":
                return "#38a169";
            case "ASSIGNED":
                return "#f39c12";
            default:
                return "#667eea";
        }
    }

    public String getTimeAgo() {
        if (activityTime == null) {
            return "Just now";
        }

        long diffMinutes = Duration.between(activityTime, LocalDateTime.now()).toMinutes();

        if (diffMinutes < 1) {
            return "Just now";
        }

        if (diffMinutes < 60) {
            return diffMinutes + " minutes ago";
        }

        if (diffMinutes < 1440) {
            return (diffMinutes / 60) + " hours ago";
        }

        return (diffMinutes / 1440) + " days ago";
    }

    public String getFormattedTime() {
        if (activityTime == null) {
            return "";
        }
        return activityTime.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    public String getFormattedDate() {
        if (activityTime == null) {
            return "";
        }
        return activityTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
}