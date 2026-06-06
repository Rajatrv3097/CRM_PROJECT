package com.softcrm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private Long id;
    private Long companyId;
    private String companyName;

    // Basic Info
    private String name;
    private String email;
    private String phone;
    private String alternativePhone;
    private String fullAddress;

    // Company Info
    private String customerCompanyName;
    private String gstNumber;
    private String panNumber;
    private String industry;
    private String website;

    // Social Media
    private String linkedinUrl;
    private String facebookUrl;
    private String twitterUrl;
    private String instagramUrl;

    // Segmentation
    private String customerType;
    private String customerTier;
    private String customerStatus;
    private List<String> tags;

    // Business Metrics
    private BigDecimal totalPurchases;
    private Integer totalOrders;
    private BigDecimal averageOrderValue;
    private LocalDateTime lastPurchaseDate;
    private BigDecimal lifetimeValue;

    // Source
    private String source;
    private String sourceDetails;

    // JSON Data
    private List<Map<String, Object>> notes;
    private List<Map<String, Object>> contacts;
    private List<Map<String, Object>> documents;
    private List<Map<String, Object>> interactions;

    // KYC
    private String kycStatus;
    private String kycDocumentUrl;

    // Assignment
    private Long assignedToId;
    private String assignedToName;

    // Timestamps
    private LocalDateTime createdAt;
}