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
@Table(name = "tickets")
public class Ticket extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;  // Changed from Customer to User

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    private LocalDateTime resolvedAt;
    private LocalDateTime slaDeadline;
    private Integer escalationLevel = 0;

    @Column(name = "ticket_number", unique = true, nullable = false)
    private String ticketNumber;

    public enum TicketStatus {
        OPEN, IN_PROGRESS, RESOLVED, CLOSED, REOPENED
    }

    public enum TicketPriority {
        LOW, MEDIUM, HIGH, URGENT
    }
}