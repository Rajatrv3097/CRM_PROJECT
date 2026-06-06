package com.softcrm.entity;

import com.softcrm.entity.base.BaseEntity;
import com.softcrm.entity.user.User;
import com.softcrm.entity.Customer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "leads", indexes = {
        @Index(name = "idx_lead_company", columnList = "company_id"),
        @Index(name = "idx_lead_status", columnList = "status"),
        @Index(name = "idx_lead_assigned", columnList = "assigned_to")
})
public class Lead extends BaseEntity {

    // ============ Tenant ============
    @Column(name = "company_id", nullable = true)
    private Long companyId;

    // ============ Basic Info ============
    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String company;

    // ============ Lead Details ============
    @Column(length = 50)
    private String status;  // NEW, CONTACTED, QUALIFIED, PROPOSAL_SENT, NEGOTIATION, CONVERTED, LOST

    @Column(length = 50)
    private String source;  // WEBSITE, FACEBOOK_ADS, GOOGLE_ADS, INSTAGRAM, WHATSAPP, REFERRAL, EMAIL_CAMPAIGN, OFFLINE_EVENTS

    private Integer score;  // Lead score 0-100

    @Column(columnDefinition = "TEXT")
    private String description;

    // ============ Assignment ============
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    // ❌ REMOVED - createdBy field (already exists in BaseEntity as String)
    // BaseEntity already has: createdBy (String), updatedBy (String)
    // BaseEntity already has: createdAt, updatedAt, isDeleted

    // ============ Followups (JSON) ==========
    @Column(columnDefinition = "TEXT")
    private String followups;  // JSON array

    // ============ Notes (JSON) ==========
    @Column(columnDefinition = "TEXT")
    private String notes;  // JSON array

    // ============ Lead History (JSON) ==========
    @Column(name = "lead_history", columnDefinition = "LONGTEXT")
    private String leadHistory;  // JSON array - tracks ALL changes

    // ============ Conversion ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_customer_id")
    private Customer convertedCustomer;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "converted_note", columnDefinition = "TEXT")
    private String convertedNote;

    // ============ Expected Value ==========
    @Column(name = "expected_revenue", precision = 15, scale = 2)
    private BigDecimal expectedRevenue;

    // ============ Helper Methods ==========
    public boolean isConverted() {
        return "CONVERTED".equals(status);
    }

    public boolean isLost() {
        return "LOST".equals(status);
    }

    public String getStatusDisplay() {
        switch (status) {
            case "NEW": return "🆕 New";
            case "CONTACTED": return "📞 Contacted";
            case "QUALIFIED": return "✅ Qualified";
            case "PROPOSAL_SENT": return "📄 Proposal Sent";
            case "NEGOTIATION": return "🤝 Negotiation";
            case "CONVERTED": return "🎉 Converted";
            case "LOST": return "❌ Lost";
            default: return status;
        }
    }

    public String getSourceDisplay() {
        switch (source) {
            case "WEBSITE": return "🌐 Website";
            case "FACEBOOK_ADS": return "📘 Facebook Ads";
            case "GOOGLE_ADS": return "🔍 Google Ads";
            case "INSTAGRAM": return "📸 Instagram";
            case "WHATSAPP": return "💬 WhatsApp";
            case "REFERRAL": return "🤝 Referral";
            case "EMAIL_CAMPAIGN": return "📧 Email Campaign";
            case "OFFLINE_EVENTS": return "🏢 Offline Events";
            default: return source;
        }
    }
}