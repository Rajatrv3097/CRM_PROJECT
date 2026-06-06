package com.softcrm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Statistics Data Transfer Object
 * Contains all data needed for dashboard display
 * Supports role-based views (SUPER_ADMIN, ADMIN, MANAGER, EMPLOYEE)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    // ============ Overview Cards ============
    private Long totalLeads;
    private Long totalCustomers;
    private Long activeDeals;
    private BigDecimal monthlyRevenue;
    private Long pendingTasks;
    private Long newTickets;

    // ============ Lead Statistics ============
    private Map<String, Long> leadByStatus;
    private Map<String, Long> leadBySource;
    private Long convertedLeads;
    private Long lostLeads;
    private Double leadConversionRate;

    // ============ Sales Statistics ============
    private Map<String, Long> dealsByStage;
    private BigDecimal totalRevenue;
    private Double conversionRate;
    private BigDecimal averageDealValue;
    private Long totalDealsClosed;
    private BigDecimal revenueGrowth;
    private Map<String, BigDecimal> revenueByRegion;
    private Map<String, BigDecimal> revenueByProduct;

    // ============ Employee Performance ============
    private List<EmployeePerformanceDTO> topEmployees;
    private List<TeamPerformanceDTO> teamPerformance;

    // ============ Support Ticket Summary ============
    private Map<String, Long> ticketsByStatus;
    private Map<String, Long> ticketsByPriority;
    private Long openTickets;
    private Long closedTickets;
    private Long highPriorityTickets;
    private Long mediumPriorityTickets;
    private Long lowPriorityTickets;
    private Double averageResolutionTime;
    private List<TicketSummaryDTO> recentTickets;

    // ============ Monthly Graphs Data ============
    private Map<String, BigDecimal> monthlySalesData;
    private Map<String, Long> monthlyLeadsData;
    private Map<String, Long> monthlyDealsClosed;
    private ChartDataDTO salesChart;
    private ChartDataDTO leadsChart;
    private ChartDataDTO revenueChart;

    // ============ Recent Activities ============
    private List<RecentActivityDTO> recentActivities;

    // ============ Notifications ============
    private List<NotificationDTO> notifications;
    private Long unreadNotifications;

    // ============ Upcoming Meetings ============
    private List<MeetingDTO> upcomingMeetings;
    private List<MeetingDTO> todayMeetings;
    private Map<String, List<MeetingDTO>> weeklyCalendar;

    // ============ Employee Personal Stats ============
    private MyPerformanceDTO myPerformance;

    // ============ SUPER ADMIN Fields ============
    private Long totalCompanies;
    private Long activeCompanies;
    private Map<String, Object> multiCompanyStats;
    private BigDecimal projectedRevenue;
    private BigDecimal targetRevenue;
    private Double targetAchievement;

    // ============ ADMIN Fields ============
    private Map<String, Object> companyInfo;
    private List<String> enabledModules;

    // ============ MANAGER Fields ============
    private Map<String, Object> teamInfo;

    // ============ Common Fields ============
    private LocalDateTime updatedAt;

    // ============ Helper Methods ============

    public boolean isComplete() {
        return totalLeads != null && totalCustomers != null &&
                activeDeals != null && monthlyRevenue != null;
    }

    public Long getTotalActiveItems() {
        return (totalLeads != null ? totalLeads : 0L) +
                (activeDeals != null ? activeDeals : 0L) +
                (pendingTasks != null ? pendingTasks : 0L) +
                (newTickets != null ? newTickets : 0L);
    }

    public String getSummary() {
        return String.format("Total Leads: %d | Customers: %d | Revenue: $%.2f | Conversion: %.1f%%",
                totalLeads != null ? totalLeads : 0L,
                totalCustomers != null ? totalCustomers : 0L,
                monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO,
                conversionRate != null ? conversionRate : 0.0);
    }

    public boolean isModuleEnabled(String moduleName) {
        return enabledModules != null && enabledModules.contains(moduleName);
    }

    public Long getTotalCompanies() {
        return totalCompanies;
    }
}