package com.softcrm.entity;

import com.softcrm.entity.base.BaseEntity;
import com.softcrm.entity.user.User;
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
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_company", columnList = "company_id"),
        @Index(name = "idx_customer_email", columnList = "email"),
        @Index(name = "idx_customer_phone", columnList = "phone"),
        @Index(name = "idx_customer_assigned", columnList = "assigned_to")
})
public class Customer extends BaseEntity {

    // ============ Tenant ============
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // ============ Basic Information ============
    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "alternative_phone", length = 20)
    private String alternativePhone;

    // ============ Address ============
    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(length = 20)
    private String pincode;

    // ============ Company Details ============
    @Column(name = "customer_company_name", length = 200)
    private String customerCompanyName;

    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    @Column(name = "pan_number", length = 50)
    private String panNumber;

    @Column(length = 100)
    private String industry;

    @Column(length = 255)
    private String website;

    // ============ Social Media ============
    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @Column(name = "facebook_url", length = 255)
    private String facebookUrl;

    @Column(name = "twitter_url", length = 255)
    private String twitterUrl;

    @Column(name = "instagram_url", length = 255)
    private String instagramUrl;

    // ============ Customer Segmentation ============
    @Column(name = "customer_type", length = 50)
    private String customerType;

    @Column(name = "customer_tier", length = 50)
    private String customerTier;

    @Column(name = "customer_status", length = 50)
    private String customerStatus;

    // ============ Tags (JSON) ============
    @Column(columnDefinition = "TEXT")
    private String tags;

    // ============ Business Metrics ============
    @Column(name = "total_purchases", precision = 15, scale = 2)
    private BigDecimal totalPurchases = BigDecimal.ZERO;

    @Column(name = "total_orders")
    private Integer totalOrders = 0;

    @Column(name = "average_order_value", precision = 10, scale = 2)
    private BigDecimal averageOrderValue = BigDecimal.ZERO;

    @Column(name = "last_purchase_date")
    private LocalDateTime lastPurchaseDate;

    @Column(name = "lifetime_value", precision = 15, scale = 2)
    private BigDecimal lifetimeValue = BigDecimal.ZERO;

    // ============ Customer Source ============
    @Column(length = 50)
    private String source;

    @Column(name = "source_details", length = 255)
    private String sourceDetails;

    // ============ Notes (JSON) ============
    @Column(columnDefinition = "TEXT")
    private String notes;

    // ============ Contacts (JSON) ============
    @Column(columnDefinition = "TEXT")
    private String contacts;

    // ============ Documents (JSON) ============
    @Column(columnDefinition = "TEXT")
    private String documents;

    // ============ Interactions (JSON) ============
    @Column(columnDefinition = "TEXT")
    private String interactions;

    // ============ KYC ============
    @Column(name = "kyc_status", length = 50)
    private String kycStatus = "PENDING";

    @Column(name = "kyc_document_url", length = 500)
    private String kycDocumentUrl;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @Column(name = "kyc_verified_by")
    private Long kycVerifiedBy;

    // ============ Assignment (Only this relationship) ============
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    // ❌ REMOVED - createdBy field (already exists in BaseEntity as String)
    // BaseEntity already has: createdBy (String), updatedBy (String)
    // BaseEntity already has: createdAt, updatedAt, isDeleted

    // ============ Helper Methods ============

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null) sb.append(address);
        if (city != null) sb.append(", ").append(city);
        if (state != null) sb.append(", ").append(state);
        if (pincode != null) sb.append(" - ").append(pincode);
        if (country != null) sb.append(", ").append(country);
        return sb.toString();
    }

    public void addPurchase(BigDecimal amount) {
        if (this.totalPurchases == null) this.totalPurchases = BigDecimal.ZERO;
        if (this.totalOrders == null) this.totalOrders = 0;

        this.totalPurchases = this.totalPurchases.add(amount);
        this.totalOrders++;
        this.lastPurchaseDate = LocalDateTime.now();

        if (this.totalOrders > 0) {
            this.averageOrderValue = this.totalPurchases.divide(BigDecimal.valueOf(this.totalOrders), 2, java.math.RoundingMode.HALF_UP);
        }

        this.lifetimeValue = this.totalPurchases;
        updateTier();
    }

    public void updateTier() {
        if (lifetimeValue == null) lifetimeValue = BigDecimal.ZERO;

        if (lifetimeValue.compareTo(BigDecimal.valueOf(100000)) >= 0) {
            this.customerTier = "PLATINUM";
        } else if (lifetimeValue.compareTo(BigDecimal.valueOf(50000)) >= 0) {
            this.customerTier = "GOLD";
        } else if (lifetimeValue.compareTo(BigDecimal.valueOf(10000)) >= 0) {
            this.customerTier = "SILVER";
        } else {
            this.customerTier = "BRONZE";
        }
    }

    public boolean isActive() {
        return "ACTIVE".equals(this.customerStatus);
    }
}