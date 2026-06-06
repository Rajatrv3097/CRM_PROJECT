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
public class MyPerformanceDTO {
    private Long myLeads;
    private Long myCustomers;
    private Long myDeals;
    private BigDecimal myRevenue;
    private Long myTasks;
    private Long myTickets;
    private Double myTargetProgress;
    private BigDecimal remainingTarget;
    private Integer myRank;
    private Double myProductivity;
}