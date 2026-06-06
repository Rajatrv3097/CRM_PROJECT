package com.softcrm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamPerformanceDTO {
    private String teamName;
    private String department;
    private Long totalMembers;
    private Long totalLeads;
    private Long totalDeals;
    private BigDecimal totalRevenue;
    private Double averageConversionRate;
}