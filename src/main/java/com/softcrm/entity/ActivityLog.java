package com.softcrm.entity;

import com.softcrm.entity.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "activity_logs")
@EntityListeners(AuditingEntityListener.class)
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "activity_type")
    private String activityType;

    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_name")
    private String entityName;

    private String description;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @CreatedDate
    @Column(name = "activity_time", updatable = false)
    private LocalDateTime activityTime;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadataJson;

    // ✅ EXPLICIT GETTER for entityId (in case Lombok fails)
    public Long getEntityId() {
        return entityId;
    }

    // ✅ EXPLICIT SETTER for entityId
    public void setEntityId(Long entityId2) {
        this.entityId = entityId2;
    }

    // ✅ Get metadata as Map
    @Transient
    public Map<String, Object> getMetadata() {
        if (metadataJson == null || metadataJson.isEmpty()) {
            return new HashMap<>();
        }
        // Parse JSON to Map (you can use Jackson ObjectMapper here)
        return new HashMap<>();
    }
}