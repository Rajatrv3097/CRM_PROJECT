package com.softcrm.entity;

import com.softcrm.entity.base.BaseEntity;
import com.softcrm.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "meetings")
public class Meeting extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String location;
    private String meetingLink;

    @Enumerated(EnumType.STRING)
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    private MeetingType type = MeetingType.INTERNAL;

    @ManyToOne
    @JoinColumn(name = "organizer_id")
    private User organizer;

    @ManyToOne
    @JoinColumn(name = "attendee_id")
    private User attendee;

    private String relatedTo; // LEAD, CUSTOMER, DEAL
    private Long relatedId;

    private String notes;

    public enum MeetingStatus {
        SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED
    }

    public enum MeetingType {
        CLIENT, INTERNAL, CALL, DEMO, PRESENTATION
    }
}