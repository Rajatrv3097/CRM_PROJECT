package com.softcrm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Employee Performance Data Transfer Object
 * Used for top performers and team performance display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePerformanceDTO {

    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;  // ✅ ADD THIS - missing field
    private String role;
    private String department;
    private String avatarUrl;

    // Performance Metrics
    private Long leadsGenerated;  // ✅ ADD THIS - for leads generated count
    private Long leadsConverted;
    private Long dealsClosed;
    private BigDecimal revenueGenerated;
    private Double customerSatisfaction;
    private Double conversionRate;
    private Double averageDealSize;

    // Additional fields from your convert method
    private Long tasksCompleted;  // ✅ ADD THIS
    private Double targetProgress;  // ✅ ADD THIS
    private Integer rank;  // ✅ ADD THIS

    // Time-based metrics
    private Long monthlyLeads;
    private Long monthlyDeals;
    private BigDecimal monthlyRevenue;

    // Performance Badge
    private String performanceBadge;  // EXCELLENT, GOOD, AVERAGE, NEEDS_IMPROVEMENT

    // Helper methods
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getPerformanceColor() {
        switch (performanceBadge != null ? performanceBadge : "AVERAGE") {
            case "EXCELLENT": return "#fbbf24";
            case "GOOD": return "#34d399";
            case "AVERAGE": return "#60a5fa";
            default: return "#f87171";
        }
    }

    public String getInitials() {
        String first = firstName != null && firstName.length() > 0 ? firstName.substring(0, 1) : "";
        String last = lastName != null && lastName.length() > 0 ? lastName.substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}