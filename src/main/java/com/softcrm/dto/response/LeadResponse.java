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
public class LeadResponse {
    private Long id;
    private Long companyId;

    // Basic Info
    private String name;
    private String email;
    private String phone;
    private String company;

    // Lead Details
    private String status;
    private String statusDisplay;
    private String source;
    private String sourceDisplay;
    private Integer score;
    private String description;

    // Assignment
    private Long assignedToId;
    private String assignedToName;
    private Long createdById;
    private String createdByName;

    // Followups & Notes
    private List<Map<String, Object>> followups;
    private List<Map<String, Object>> notes;

    // ✅ LEAD HISTORY - All changes tracked
    private List<Map<String, Object>> leadHistory;

    // Conversion
    private Long convertedCustomerId;
    private String convertedCustomerName;
    private LocalDateTime convertedAt;

    // Value
    private BigDecimal expectedRevenue;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}