package com.softcrm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDTO {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String meetingLink;
    private String status;          // SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED
    private String meetingType;      // ✅ CHANGED from 'type' to 'meetingType' to match DashboardStatsDTO
    private List<String> attendees;  // ✅ ADDED - replaces withUserName/withUserId
    private String organizer;        // ✅ ADDED - replaces organizerName/organizerId
    private String color;            // ✅ ADDED - for UI styling
    private Boolean isRecurring;     // ✅ ADDED
    private String recurrencePattern; // ✅ ADDED

    // Keep these if needed internally, or remove
    // private String withUserName;
    // private Long withUserId;
    // private String organizerName;
    // private Long organizerId;
    private String notes;
    private String relatedTo;        // LEAD, CUSTOMER, DEAL
    private Long relatedId;
    private String relatedName;

    // Helper methods (keep as is or adjust)
    public boolean isToday() {
        if (startTime == null) return false;
        return startTime.toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }

    public boolean isTomorrow() {
        if (startTime == null) return false;
        return startTime.toLocalDate().equals(LocalDateTime.now().toLocalDate().plusDays(1));
    }

    public String getFormattedTime() {
        if (startTime == null) return "TBD";
        return startTime.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    public String getFormattedDate() {
        if (startTime == null) return "TBD";
        return startTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }

    public String getDuration() {
        if (startTime == null || endTime == null) return "TBD";
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes < 60) return minutes + " min";
        return (minutes / 60) + " hr " + (minutes % 60) + " min";
    }

    public String getStatusBadge() {
        switch (status != null ? status : "SCHEDULED") {
            case "SCHEDULED": return "upcoming";
            case "COMPLETED": return "completed";
            case "CANCELLED": return "cancelled";
            default: return "rescheduled";
        }
    }

    public String getStatusColor() {
        switch (status != null ? status : "SCHEDULED") {
            case "SCHEDULED": return "#48bb78";
            case "COMPLETED": return "#3498db";
            case "CANCELLED": return "#f56565";
            default: return "#f39c12";
        }
    }

    public String getMeetingTypeIcon() {  // ✅ RENAMED from getTypeIcon
        switch (meetingType != null ? meetingType : "INTERNAL") {
            case "CLIENT": return "fa-user-tie";
            case "INTERNAL": return "fa-users";
            case "CALL": return "fa-phone";
            case "DEMO": return "fa-laptop";
            case "PRESENTATION": return "fa-chalkboard";
            default: return "fa-calendar";
        }
    }
}