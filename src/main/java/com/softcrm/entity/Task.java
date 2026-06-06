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
@Table(name = "tasks")
public class Task extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    // ✅ DON'T use @CreatedBy - BaseEntity already has createdBy (String)
    // ✅ DON'T create getCreatedBy() method - it conflicts with BaseEntity

    // Instead use these fields with different names
    @ManyToOne
    @JoinColumn(name = "created_by_user")
    private User createdByUser;  // ✅ Different name - no conflict

    private LocalDateTime dueDate;
    private LocalDateTime completedAt;

    private String relatedTo; // LEAD, CUSTOMER, DEAL
    private Long relatedId;

    // ✅ Custom field for assigner (who assigned the task)
    @ManyToOne
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    public enum TaskPriority {
        LOW, MEDIUM, HIGH, URGENT
    }

    public enum TaskStatus {
        PENDING, IN_PROGRESS, COMPLETED, ON_HOLD, CANCELLED
    }
}