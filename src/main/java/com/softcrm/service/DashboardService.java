package com.softcrm.service;

import com.softcrm.dto.response.*;
import com.softcrm.dto.response.DashboardStatsDTO.*;
import com.softcrm.entity.*;
import com.softcrm.entity.user.User;
import com.softcrm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final DealRepository dealRepository;
    private final TaskRepository taskRepository;
    private final TicketRepository ticketRepository;
    private final MeetingRepository meetingRepository;
    private final ActivityLogRepository activityLogRepository;
    private final CompanyRepository companyRepository;
    private final NotificationRepository notificationRepository;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    // ============ Helper method to get current user company ID ============
    private Long getCurrentUserCompanyId() {
        // This should be implemented based on your security context
        // For now, return null (super admin view)
        return null;
    }

    // ============ SUPER ADMIN DASHBOARD ============
    @Transactional(readOnly = true)
    public DashboardStatsDTO getSuperAdminDashboard() {
        log.info("Fetching SUPER_ADMIN dashboard data");

        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<Company> companies = companyRepository.findAll();

        Map<String, Object> multiCompanyStats = new LinkedHashMap<>();
        for (Company company : companies) {
            Map<String, Object> companyStats = new HashMap<>();
            companyStats.put("name", company.getName());
            companyStats.put("code", company.getCode());
            companyStats.put("subscriptionPlan", company.getSubscriptionPlan());
            companyStats.put("isActive", company.getIsActive());

            if (company.isModuleEnabled("LEAD_MANAGEMENT")) {
                companyStats.put("totalLeads", leadRepository.countByCompanyId(company.getId()));
            }
            if (company.isModuleEnabled("SALES_PIPELINE")) {
                companyStats.put("monthlyRevenue", dealRepository.getMonthlyRevenueByCompany(company.getId()));
            }
            companyStats.put("userCount", userRepository.countActiveUsersByCompany(company.getId()));
            companyStats.put("enabledModules", company.getEnabledModules());

            multiCompanyStats.put(company.getCode(), companyStats);
        }

        return DashboardStatsDTO.builder()
                .totalCompanies((long) companies.size())
                .activeCompanies(companies.stream().filter(Company::getIsActive).count())
                .totalLeads(leadRepository.count())
                .activeDeals(dealRepository.countByStatus(Deal.DealStatus.ACTIVE))
                .monthlyRevenue(dealRepository.getMonthlyRevenue())
                .pendingTasks(taskRepository.countByStatus(Task.TaskStatus.PENDING))
                .newTickets(ticketRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(7)))
                .totalRevenue(dealRepository.getTotalRevenue())
                .averageDealValue(getAverageDealValue())
                .totalDealsClosed(dealRepository.countClosedWonDeals())
                .revenueGrowth(getRevenueGrowth())
                .conversionRate(calculateConversionRate(leadRepository.count(), getConvertedLeadsCount()))
                .leadByStatus(convertToMap(leadRepository.getLeadCountByStatus(getCurrentUserCompanyId())))
                .leadBySource(convertToMap(leadRepository.getLeadCountBySource(getCurrentUserCompanyId())))
                .dealsByStage(convertToMap(dealRepository.getDealCountByStage()))
                .ticketsByStatus(convertToMap(ticketRepository.getTicketCountByStatus()))
                .ticketsByPriority(convertToMap(ticketRepository.getTicketCountByPriority()))
                .openTickets(ticketRepository.countByStatus(Ticket.TicketStatus.OPEN))
                .closedTickets(ticketRepository.countByStatus(Ticket.TicketStatus.CLOSED))
                .highPriorityTickets(ticketRepository.countByPriority(Ticket.TicketPriority.HIGH))
                .mediumPriorityTickets(ticketRepository.countByPriority(Ticket.TicketPriority.MEDIUM))
                .lowPriorityTickets(ticketRepository.countByPriority(Ticket.TicketPriority.LOW))
                .averageResolutionTime(getAverageResolutionTime())
                .monthlySalesData(convertToMonthlyMap(dealRepository.getMonthlySalesData(sixMonthsAgo)))
                .monthlyLeadsData(convertToMonthlyLongMap(getMonthlyLeadsData(sixMonthsAgo)))
                .monthlyDealsClosed(convertToMonthlyLongMap(dealRepository.getMonthlyDealsClosedData(sixMonthsAgo)))
                .topEmployees(getTopPerformersGlobal())
                .teamPerformance(getTeamPerformance())
                .recentTickets(getRecentTickets(5))
                .recentActivities(getRecentActivities(activityLogRepository.findTop20ByOrderByActivityTimeDesc()))
                .notifications(getNotifications(10))
                .unreadNotifications(notificationRepository.countByIsReadFalse())
                .upcomingMeetings(getUpcomingMeetingsByCompany(null))
                .todayMeetings(getTodayMeetings())
                .weeklyCalendar(getWeeklyCalendar())
                .revenueByRegion(getRevenueByRegion())
                .revenueByProduct(getRevenueByProduct())
                .projectedRevenue(getProjectedRevenue())
                .targetRevenue(getTargetRevenue())
                .targetAchievement(getTargetAchievement())
                .multiCompanyStats(multiCompanyStats)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ============ ADMIN DASHBOARD ============
    @Transactional(readOnly = true)
    public DashboardStatsDTO getAdminDashboard(String email) {
        log.info("Fetching ADMIN dashboard data for: {}", email);

        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long companyId = admin.getCompanyId();
        if (companyId == null) {
            log.warn("Admin {} has no company assigned", email);
            return getEmptyDashboard();
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        if (!company.getIsActive()) {
            log.warn("Company {} is not active", company.getName());
            return getEmptyDashboard();
        }

        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        DashboardStatsDTO.DashboardStatsDTOBuilder builder = DashboardStatsDTO.builder()
                .companyInfo(getCompanyInfo(company))
                .enabledModules(company.getEnabledModules())
                .totalRevenue(dealRepository.getTotalRevenueByCompany(companyId))
                .monthlyRevenue(dealRepository.getMonthlyRevenueByCompany(companyId))
                .averageDealValue(getAverageDealValueByCompany(companyId))
                .revenueGrowth(getRevenueGrowthByCompany(companyId))
                .ticketsByStatus(convertToMap(ticketRepository.getTicketCountByStatusByCompany(companyId)))
                .ticketsByPriority(convertToMap(ticketRepository.getTicketCountByPriorityByCompany(companyId)))
                .openTickets(ticketRepository.countByCompanyIdAndStatus(companyId, Ticket.TicketStatus.OPEN))
                .closedTickets(ticketRepository.countByCompanyIdAndStatus(companyId, Ticket.TicketStatus.CLOSED))
                .recentTickets(getRecentTicketsByCompany(companyId, 5))
                .recentActivities(getRecentActivities(activityLogRepository.findTop20ByCompanyId(companyId)))
                .notifications(getNotificationsByUser(admin.getId(), 10))
                .unreadNotifications(notificationRepository.countByUserIdAndIsReadFalse(admin.getId()))
                .upcomingMeetings(getUpcomingMeetingsByCompany(companyId))
                .todayMeetings(getTodayMeetingsByCompany(companyId))
                .weeklyCalendar(getWeeklyCalendarByCompany(companyId));

        if (company.isModuleEnabled("LEAD_MANAGEMENT")) {
            builder.totalLeads(leadRepository.countByCompanyId(companyId))
                    .leadByStatus(convertToMap(leadRepository.getLeadCountByStatus(companyId)))
                    .leadBySource(convertToMap(leadRepository.getLeadCountBySource(companyId)))
                    .monthlyLeadsData(convertToMonthlyLongMap(leadRepository.getMonthlyLeadsDataByCompany(companyId, sixMonthsAgo)))
                    .conversionRate(calculateConversionRateForCompany(companyId))
                    .convertedLeads(leadRepository.countByCompanyIdAndStatusConverted(companyId))
                    .lostLeads(leadRepository.countByCompanyIdAndStatusLost(companyId));
        }

        if (company.isModuleEnabled("SALES_PIPELINE")) {
            builder.activeDeals(dealRepository.countByCompanyIdAndStatus(companyId, Deal.DealStatus.ACTIVE))
                    .dealsByStage(convertToMap(dealRepository.getDealCountByStageByCompany(companyId)))
                    .monthlySalesData(convertToMonthlyMap(dealRepository.getMonthlySalesDataByCompany(companyId, sixMonthsAgo)))
                    .totalDealsClosed(dealRepository.countClosedWonDealsByCompany(companyId));
        }

        if (company.isModuleEnabled("HR_MODULE")) {
            builder.topEmployees(getTopPerformersByCompany(companyId))
                    .pendingTasks(taskRepository.countByCompanyIdAndStatus(companyId, Task.TaskStatus.PENDING))
                    .myPerformance(getMyPerformance(admin.getId()));
        }

        if (company.isModuleEnabled("SUPPORT_TICKETS")) {
            builder.newTickets(ticketRepository.countByCompanyIdAndStatus(companyId, Ticket.TicketStatus.OPEN));
        }

        if (company.isModuleEnabled("REPORTS")) {
            builder.revenueByRegion(getRevenueByRegionForCompany(companyId))
                    .revenueByProduct(getRevenueByProductForCompany(companyId));
        }

        return builder.build();
    }

    // ============ MANAGER DASHBOARD ============
    @Transactional(readOnly = true)
    public DashboardStatsDTO getManagerDashboard(String email) {
        log.info("Fetching MANAGER dashboard data for: {}", email);

        User manager = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long managerId = manager.getId();
        Long companyId = manager.getCompanyId();

        Company company = companyRepository.findById(companyId).orElse(null);
        boolean hrModuleEnabled = company != null && company.isModuleEnabled("HR_MODULE");

        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        DashboardStatsDTO.DashboardStatsDTOBuilder builder = DashboardStatsDTO.builder()
                .teamInfo(getTeamInfo(manager))
                .recentActivities(getRecentActivities(activityLogRepository.findTop20ByTeamId(managerId)))
                .upcomingMeetings(getUpcomingMeetingsByTeam(managerId))
                .notifications(getNotificationsByUser(manager.getId(), 10))
                .unreadNotifications(notificationRepository.countByUserIdAndIsReadFalse(manager.getId()));

        if (hrModuleEnabled) {
            builder.totalLeads(leadRepository.countByTeamId(managerId))
                    .activeDeals(dealRepository.countByTeamIdAndStatus(managerId, Deal.DealStatus.ACTIVE))
                    .monthlyRevenue(dealRepository.getMonthlyRevenueByTeam(managerId))
                    .pendingTasks(taskRepository.countByTeamIdAndStatus(managerId, Task.TaskStatus.PENDING))
                    .newTickets(ticketRepository.countByTeamIdAndStatus(managerId, Ticket.TicketStatus.OPEN))
                    .totalRevenue(dealRepository.getTotalRevenueByTeam(managerId))
                    .averageDealValue(getAverageDealValueByTeam(managerId))
                    .conversionRate(calculateConversionRateForTeam(managerId))
                    .topEmployees(getTopPerformersByTeam(managerId))
                    .myPerformance(getMyPerformance(manager.getId()))
                    .teamPerformance(getTeamPerformanceByUsers(getTeamMemberIds(managerId)))
                    .monthlySalesData(convertToMonthlyMap(dealRepository.getMonthlySalesDataByTeam(managerId, sixMonthsAgo)))
                    .monthlyLeadsData(convertToMonthlyLongMap(leadRepository.getMonthlyLeadsDataByTeam(managerId, sixMonthsAgo)));
        }

        return builder.build();
    }

    // ============ EMPLOYEE DASHBOARD ============
    @Transactional(readOnly = true)
    public DashboardStatsDTO getEmployeeDashboard(String email) {
        log.info("Fetching EMPLOYEE dashboard data for: {}", email);

        User employee = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long userId = employee.getId();
        Long companyId = employee.getCompanyId();

        Company company = companyRepository.findById(companyId).orElse(null);
        boolean hrModuleEnabled = company != null && company.isModuleEnabled("HR_MODULE");

        Double monthlyTarget = getEmployeeMonthlyTarget(employee);
        BigDecimal achievedRevenue = dealRepository.getUserMonthlyRevenue(userId);
        double progress = monthlyTarget > 0 ? (achievedRevenue.doubleValue() / monthlyTarget) * 100 : 0;

        MyPerformanceDTO myPerformance = MyPerformanceDTO.builder()
                .myLeads(leadRepository.countByAssignedTo(userId))
                .myDeals(dealRepository.countByOwnerIdAndStatus(userId, Deal.DealStatus.ACTIVE))
                .myRevenue(dealRepository.getUserRevenue(userId))
                .myTasks(taskRepository.countByAssignedToIdAndStatus(userId, Task.TaskStatus.PENDING))
                .myTickets(ticketRepository.countByAssignedToAndStatus(userId, Ticket.TicketStatus.OPEN))
                .myTargetProgress(progress)
                .remainingTarget(BigDecimal.valueOf(Math.max(0, monthlyTarget - achievedRevenue.doubleValue())))
                .build();

        if (hrModuleEnabled) {
            myPerformance.setMyRank(getEmployeeRank(userId, companyId));
            myPerformance.setMyProductivity(calculateProductivityScore(employee));
        }

        DashboardStatsDTO.DashboardStatsDTOBuilder builder = DashboardStatsDTO.builder()
                .myPerformance(myPerformance)
                .recentActivities(getRecentActivities(activityLogRepository.findTop20ByUserId(userId)))
                .notifications(getNotificationsByUser(userId, 10))
                .unreadNotifications(notificationRepository.countByUserIdAndIsReadFalse(userId))
                .upcomingMeetings(getUpcomingMeetingsByUser(userId))
                .todayMeetings(getTodayMeetingsByUser(userId));

        if (hrModuleEnabled) {
            builder.totalLeads(leadRepository.countByAssignedTo(userId))
                    .activeDeals(dealRepository.countByOwnerIdAndStatus(userId, Deal.DealStatus.ACTIVE))
                    .monthlyRevenue(dealRepository.getUserMonthlyRevenue(userId))
                    .pendingTasks(taskRepository.countByAssignedToIdAndStatus(userId, Task.TaskStatus.PENDING))
                    .newTickets(ticketRepository.countByAssignedToAndStatus(userId, Ticket.TicketStatus.OPEN))
                    .monthlySalesData(convertToMonthlyMap(dealRepository.getUserMonthlySalesData(userId, LocalDateTime.now().minusMonths(6))))
                    .monthlyLeadsData(convertToMonthlyLongMap(leadRepository.getUserMonthlyLeadsData(userId, LocalDateTime.now().minusMonths(6))));
        }

        return builder.build();
    }

    // ============ AUTO DETECT DASHBOARD ============
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardByUserRole(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userType = user.getUserType();
        log.info("Auto-detecting dashboard for user type: '{}'", userType);

        if (userType == null) {
            log.warn("User type is null for email: {}, defaulting to EMPLOYEE dashboard", email);
            return getEmployeeDashboard(email);
        }

        String normalizedType = userType.toUpperCase();

        switch (normalizedType) {
            case "SUPER_ADMIN":
            case "SUPERADMIN":
                log.info("✅ Routing to SUPER_ADMIN dashboard");
                return getSuperAdminDashboard();
            case "ADMIN":
                log.info("✅ Routing to ADMIN dashboard");
                return getAdminDashboard(email);
            case "MANAGER":
                log.info("✅ Routing to MANAGER dashboard");
                return getManagerDashboard(email);
            case "EMPLOYEE":
                log.info("✅ Routing to EMPLOYEE dashboard");
                return getEmployeeDashboard(email);
            case "CUSTOMER":
                log.info("✅ Routing to CUSTOMER dashboard");
                return getEmployeeDashboard(email);
            default:
                log.warn("⚠️ Unknown user type: '{}', falling back to employee dashboard", userType);
                return getEmployeeDashboard(email);
        }
    }

    // ============ Helper Methods for Lead (since using String status) ============

    private long getConvertedLeadsCount() {
        // Get count of leads with status "CONVERTED"
        return leadRepository.countByStatus(null, "CONVERTED");
    }

    private List<Map<String, Object>> getMonthlyLeadsData(LocalDateTime startDate) {
        // This should be implemented based on your repository
        return new ArrayList<>();
    }

    // ============ PRIVATE HELPER METHODS ============

    private DashboardStatsDTO getEmptyDashboard() {
        return DashboardStatsDTO.builder()
                .totalLeads(0L).activeDeals(0L).monthlyRevenue(BigDecimal.ZERO)
                .pendingTasks(0L).newTickets(0L).leadByStatus(new HashMap<>()).leadBySource(new HashMap<>())
                .dealsByStage(new HashMap<>()).totalRevenue(BigDecimal.ZERO).averageDealValue(BigDecimal.ZERO)
                .totalDealsClosed(0L).revenueGrowth(BigDecimal.ZERO).conversionRate(0.0)
                .topEmployees(new ArrayList<>()).teamPerformance(new ArrayList<>()).ticketsByStatus(new HashMap<>())
                .ticketsByPriority(new HashMap<>()).openTickets(0L).closedTickets(0L).highPriorityTickets(0L)
                .mediumPriorityTickets(0L).lowPriorityTickets(0L).averageResolutionTime(0.0)
                .monthlySalesData(new HashMap<>()).monthlyLeadsData(new HashMap<>()).monthlyDealsClosed(new HashMap<>())
                .recentTickets(new ArrayList<>()).recentActivities(new ArrayList<>()).notifications(new ArrayList<>())
                .unreadNotifications(0L).upcomingMeetings(new ArrayList<>()).todayMeetings(new ArrayList<>())
                .weeklyCalendar(new HashMap<>()).revenueByRegion(new HashMap<>()).revenueByProduct(new HashMap<>())
                .projectedRevenue(BigDecimal.ZERO).targetRevenue(BigDecimal.ZERO).targetAchievement(0.0)
                .build();
    }

    private Map<String, Object> getCompanyInfo(Company company) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", company.getId());
        info.put("name", company.getName());
        info.put("code", company.getCode());
        info.put("subscriptionPlan", company.getSubscriptionPlan());
        info.put("maxUsers", company.getMaxUsers());
        info.put("currentUsers", userRepository.countActiveUsersByCompany(company.getId()));
        info.put("isActive", company.getIsActive());
        info.put("subscriptionExpiry", company.getSubscriptionExpiry());
        info.put("enabledModules", company.getEnabledModules());
        return info;
    }

    private Map<String, Object> getTeamInfo(User manager) {
        Map<String, Object> info = new HashMap<>();
        info.put("managerId", manager.getId());
        info.put("managerName", manager.getFullName());
        info.put("teamName", manager.getTeamName() != null ? manager.getTeamName() : "Team");
        info.put("teamSize", userRepository.countByManagerId(manager.getId()));
        info.put("department", manager.getDepartment());
        return info;
    }

    private List<Long> getTeamMemberIds(Long managerId) {
        return userRepository.findByManagerId(managerId).stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    // ============ REVENUE METHODS ============

    private BigDecimal getRevenueGrowth() {
        LocalDateTime currentMonthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime previousMonthStart = currentMonthStart.minusMonths(1);
        LocalDateTime previousMonthEnd = currentMonthStart.minusDays(1);

        BigDecimal currentRevenue = dealRepository.getRevenueBetween(currentMonthStart, LocalDateTime.now());
        BigDecimal previousRevenue = dealRepository.getRevenueBetween(previousMonthStart, previousMonthEnd);

        if (previousRevenue == null || previousRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return currentRevenue != null && currentRevenue.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return currentRevenue.subtract(previousRevenue)
                .divide(previousRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal getRevenueGrowthByCompany(Long companyId) {
        LocalDateTime currentMonthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime previousMonthStart = currentMonthStart.minusMonths(1);
        LocalDateTime previousMonthEnd = currentMonthStart.minusDays(1);

        BigDecimal currentRevenue = dealRepository.getMonthlyRevenueByCompanyAndDateRange(companyId, currentMonthStart, LocalDateTime.now());
        BigDecimal previousRevenue = dealRepository.getMonthlyRevenueByCompanyAndDateRange(companyId, previousMonthStart, previousMonthEnd);

        if (previousRevenue == null || previousRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return currentRevenue != null && currentRevenue.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return currentRevenue.subtract(previousRevenue)
                .divide(previousRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal getAverageDealValue() {
        Long totalDeals = dealRepository.countClosedWonDeals();
        BigDecimal totalRevenue = dealRepository.getTotalRevenue();
        if (totalDeals > 0 && totalRevenue != null) {
            return totalRevenue.divide(BigDecimal.valueOf(totalDeals), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getAverageDealValueByCompany(Long companyId) {
        Long totalDeals = dealRepository.countClosedWonDealsByCompany(companyId);
        BigDecimal totalRevenue = dealRepository.getTotalRevenueByCompany(companyId);
        if (totalDeals > 0 && totalRevenue != null) {
            return totalRevenue.divide(BigDecimal.valueOf(totalDeals), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getAverageDealValueByTeam(Long teamId) {
        Long totalDeals = dealRepository.countClosedWonDealsByTeam(teamId);
        BigDecimal totalRevenue = dealRepository.getTotalRevenueByTeam(teamId);
        if (totalDeals > 0 && totalRevenue != null) {
            return totalRevenue.divide(BigDecimal.valueOf(totalDeals), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private Map<String, BigDecimal> getRevenueByRegion() {
        List<Map<String, Object>> results = dealRepository.getRevenueByRegion();
        Map<String, BigDecimal> revenueMap = new LinkedHashMap<>();
        for (Map<String, Object> result : results) {
            revenueMap.put(result.get("city").toString(), new BigDecimal(result.get("revenue").toString()));
        }
        return revenueMap;
    }

    private Map<String, BigDecimal> getRevenueByRegionForCompany(Long companyId) {
        List<Map<String, Object>> results = dealRepository.getRevenueByRegionForCompany(companyId);
        Map<String, BigDecimal> revenueMap = new LinkedHashMap<>();
        for (Map<String, Object> result : results) {
            revenueMap.put(result.get("city").toString(), new BigDecimal(result.get("revenue").toString()));
        }
        return revenueMap;
    }

    private Map<String, BigDecimal> getRevenueByProduct() {
        List<Map<String, Object>> results = dealRepository.getRevenueByProduct();
        Map<String, BigDecimal> revenueMap = new LinkedHashMap<>();
        for (Map<String, Object> result : results) {
            revenueMap.put(result.get("product").toString(), new BigDecimal(result.get("revenue").toString()));
        }
        return revenueMap;
    }

    private Map<String, BigDecimal> getRevenueByProductForCompany(Long companyId) {
        List<Map<String, Object>> results = dealRepository.getRevenueByProductForCompany(companyId);
        Map<String, BigDecimal> revenueMap = new LinkedHashMap<>();
        for (Map<String, Object> result : results) {
            revenueMap.put(result.get("product").toString(), new BigDecimal(result.get("revenue").toString()));
        }
        return revenueMap;
    }

    private BigDecimal getProjectedRevenue() {
        BigDecimal currentMonthRevenue = dealRepository.getMonthlyRevenue();
        int daysPassed = LocalDate.now().getDayOfMonth();
        int totalDays = YearMonth.now().lengthOfMonth();
        if (daysPassed > 0) {
            return currentMonthRevenue.multiply(BigDecimal.valueOf(totalDays))
                    .divide(BigDecimal.valueOf(daysPassed), 2, RoundingMode.HALF_UP);
        }
        return currentMonthRevenue;
    }

    private BigDecimal getTargetRevenue() {
        return BigDecimal.valueOf(1000000);
    }

    private Double getTargetAchievement() {
        BigDecimal target = getTargetRevenue();
        BigDecimal achieved = dealRepository.getTotalRevenue();
        if (target.compareTo(BigDecimal.ZERO) > 0) {
            return achieved.divide(target, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        return 0.0;
    }

    private Double getAverageResolutionTime() {
        return ticketRepository.getAverageResolutionTime();
    }

    // ============ TICKET METHODS ============

    private List<TicketSummaryDTO> getRecentTickets(int limit) {
        return ticketRepository.findTopNByOrderByCreatedAtDesc(limit).stream()
                .map(this::convertToTicketSummary)
                .collect(Collectors.toList());
    }

    private List<TicketSummaryDTO> getRecentTicketsByCompany(Long companyId, int limit) {
        return ticketRepository.findTopNByCompanyIdOrderByCreatedAtDesc(companyId, limit).stream()
                .map(this::convertToTicketSummary)
                .collect(Collectors.toList());
    }

    private TicketSummaryDTO convertToTicketSummary(Ticket ticket) {
        String customerName = "N/A";
        if (ticket.getCustomer() != null) {
            customerName = ticket.getCustomer().getFullName();
        }

        String assignedTo = "Unassigned";
        if (ticket.getAssignedTo() != null) {
            assignedTo = ticket.getAssignedTo().getUsername();
        }

        return TicketSummaryDTO.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .title(ticket.getTitle())
                .status(ticket.getStatus() != null ? ticket.getStatus().toString() : "UNKNOWN")
                .priority(ticket.getPriority() != null ? ticket.getPriority().toString() : "UNKNOWN")
                .customerName(customerName)
                .createdAt(ticket.getCreatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .assignedTo(assignedTo)
                .build();
    }

    // ============ NOTIFICATION METHODS ============

    private List<NotificationDTO> getNotifications(int limit) {
        return notificationRepository.findTopNByOrderByCreatedAtDesc(limit).stream()
                .map(this::convertToNotification)
                .collect(Collectors.toList());
    }

    private List<NotificationDTO> getNotificationsByUser(Long userId, int limit) {
        return notificationRepository.findTopNByUserIdOrderByCreatedAtDesc(userId, limit).stream()
                .map(this::convertToNotification)
                .collect(Collectors.toList());
    }

    private NotificationDTO convertToNotification(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .icon(getNotificationIcon(notification.getType()))
                .backgroundColor(getNotificationColor(notification.getType()))
                .build();
    }

    private String getNotificationIcon(String type) {
        switch (type) {
            case "SUCCESS": return "fa-check-circle";
            case "WARNING": return "fa-exclamation-triangle";
            case "ERROR": return "fa-times-circle";
            default: return "fa-info-circle";
        }
    }

    private String getNotificationColor(String type) {
        switch (type) {
            case "SUCCESS": return "#48bb78";
            case "WARNING": return "#ed8936";
            case "ERROR": return "#f56565";
            default: return "#4299e1";
        }
    }

    // ============ MEETING & CALENDAR METHODS ============

    private List<MeetingDTO> getTodayMeetings() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        return meetingRepository.findByStartTimeBetween(start, end).stream()
                .map(this::convertToMeetingDTO)
                .collect(Collectors.toList());
    }

    private List<MeetingDTO> getTodayMeetingsByCompany(Long companyId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        return meetingRepository.findByCompanyIdAndStartTimeBetween(companyId, start, end).stream()
                .map(this::convertToMeetingDTO)
                .collect(Collectors.toList());
    }

    private List<MeetingDTO> getTodayMeetingsByUser(Long userId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        return meetingRepository.findByUserIdAndStartTimeBetween(userId, start, end).stream()
                .map(this::convertToMeetingDTO)
                .collect(Collectors.toList());
    }

    private Map<String, List<MeetingDTO>> getWeeklyCalendar() {
        Map<String, List<MeetingDTO>> calendar = new LinkedHashMap<>();
        LocalDate startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            String dayName = date.getDayOfWeek().toString();
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            List<MeetingDTO> meetings = meetingRepository.findByStartTimeBetween(dayStart, dayEnd).stream()
                    .map(this::convertToMeetingDTO)
                    .collect(Collectors.toList());
            calendar.put(dayName, meetings);
        }
        return calendar;
    }

    private Map<String, List<MeetingDTO>> getWeeklyCalendarByCompany(Long companyId) {
        Map<String, List<MeetingDTO>> calendar = new LinkedHashMap<>();
        LocalDate startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            String dayName = date.getDayOfWeek().toString();
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            List<MeetingDTO> meetings = meetingRepository.findByCompanyIdAndStartTimeBetween(companyId, dayStart, dayEnd).stream()
                    .map(this::convertToMeetingDTO)
                    .collect(Collectors.toList());
            calendar.put(dayName, meetings);
        }
        return calendar;
    }

    private List<MeetingDTO> getUpcomingMeetingsByCompany(Long companyId) {
        List<Meeting> meetings = meetingRepository.findUpcomingByCompany(companyId, LocalDateTime.now());
        return meetings.stream().map(this::convertToMeetingDTO).collect(Collectors.toList());
    }

    private List<MeetingDTO> getUpcomingMeetingsByTeam(Long teamId) {
        List<Meeting> meetings = meetingRepository.findUpcomingByTeam(teamId, LocalDateTime.now());
        return meetings.stream().map(this::convertToMeetingDTO).collect(Collectors.toList());
    }

    private List<MeetingDTO> getUpcomingMeetingsByUser(Long userId) {
        List<Meeting> meetings = meetingRepository.findUpcomingByUserId(userId);
        return meetings.stream().map(this::convertToMeetingDTO).collect(Collectors.toList());
    }

    private MeetingDTO convertToMeetingDTO(Meeting meeting) {
        String organizer = null;
        if (meeting.getOrganizer() != null) {
            organizer = meeting.getOrganizer().getUsername();
        }

        return MeetingDTO.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .location(meeting.getLocation())
                .meetingLink(meeting.getMeetingLink())
                .status(meeting.getStatus() != null ? meeting.getStatus().name() : "SCHEDULED")
                .meetingType(meeting.getType() != null ? meeting.getType().name() : "INTERNAL")
                .organizer(organizer)
                .color(getMeetingColor(meeting.getType() != null ? meeting.getType().name() : null))
                .build();
    }

    private String getMeetingColor(String type) {
        if (type == null) return "#667eea";
        switch (type) {
            case "CLIENT": return "#48bb78";
            case "INTERNAL": return "#4299e1";
            case "URGENT": return "#f56565";
            default: return "#667eea";
        }
    }

    // ============ PERFORMANCE & RANKING METHODS ============

    private MyPerformanceDTO getMyPerformance(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return MyPerformanceDTO.builder()
                .myLeads(leadRepository.countByAssignedTo(userId))
                .myDeals(dealRepository.countByOwnerIdAndStatus(userId, Deal.DealStatus.ACTIVE))
                .myRevenue(dealRepository.getUserRevenue(userId))
                .myTasks(taskRepository.countByAssignedToIdAndStatus(userId, Task.TaskStatus.PENDING))
                .myTickets(ticketRepository.countByAssignedToAndStatus(userId, Ticket.TicketStatus.OPEN))
                .myTargetProgress(getMyTargetProgress(userId))
                .remainingTarget(getRemainingTarget(userId))
                .build();
    }

    private Double getMyTargetProgress(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        Double monthlyTarget = getEmployeeMonthlyTarget(user);
        BigDecimal achievedRevenue = dealRepository.getUserMonthlyRevenue(userId);
        if (monthlyTarget > 0) {
            return (achievedRevenue.doubleValue() / monthlyTarget) * 100;
        }
        return 0.0;
    }

    private BigDecimal getRemainingTarget(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        Double monthlyTarget = getEmployeeMonthlyTarget(user);
        BigDecimal achievedRevenue = dealRepository.getUserMonthlyRevenue(userId);
        return BigDecimal.valueOf(Math.max(0, monthlyTarget - achievedRevenue.doubleValue()));
    }

    private Double getEmployeeMonthlyTarget(User employee) {
        if ("MANAGER".equals(employee.getUserType())) {
            return 100000.0;
        }
        return 50000.0;
    }

    private Integer getEmployeeRank(Long userId, Long companyId) {
        List<User> employees = userRepository.findByCompanyIdAndUserType(companyId, "EMPLOYEE");
        List<Long> rankedUsers = employees.stream()
                .sorted((a, b) -> dealRepository.getUserRevenue(b.getId()).compareTo(dealRepository.getUserRevenue(a.getId())))
                .map(User::getId)
                .collect(Collectors.toList());
        int rank = rankedUsers.indexOf(userId) + 1;
        return rank > 0 ? rank : null;
    }

    private Double calculateProductivityScore(User employee) {
        long completedTasks = taskRepository.countByAssignedToIdAndStatus(employee.getId(), Task.TaskStatus.COMPLETED);
        long totalTasks = taskRepository.countByAssignedToId(employee.getId());
        if (totalTasks == 0) return 0.0;
        return (completedTasks * 100.0) / totalTasks;
    }

    // ============ TOP PERFORMERS METHODS ============

    private List<EmployeePerformanceDTO> getTopPerformersGlobal() {
        List<User> topUsers = userRepository.findTop10ByOrderByTotalRevenueDesc();
        List<EmployeePerformanceDTO> performers = new ArrayList<>();
        int rank = 1;
        for (User user : topUsers) {
            EmployeePerformanceDTO dto = convertToEmployeePerformanceDTO(user);
            dto.setRank(rank++);
            performers.add(dto);
        }
        return performers;
    }

    private List<EmployeePerformanceDTO> getTopPerformersByCompany(Long companyId) {
        List<User> topUsers = userRepository.findTop10ByCompanyIdOrderByTotalRevenueDesc(companyId);
        List<EmployeePerformanceDTO> performers = new ArrayList<>();
        int rank = 1;
        for (User user : topUsers) {
            EmployeePerformanceDTO dto = convertToEmployeePerformanceDTO(user);
            dto.setRank(rank++);
            performers.add(dto);
        }
        return performers;
    }

    private List<EmployeePerformanceDTO> getTopPerformersByTeam(Long teamId) {
        List<User> topUsers = userRepository.findTop10ByManagerIdOrderByTotalRevenueDesc(teamId);
        List<EmployeePerformanceDTO> performers = new ArrayList<>();
        int rank = 1;
        for (User user : topUsers) {
            EmployeePerformanceDTO dto = convertToEmployeePerformanceDTO(user);
            dto.setRank(rank++);
            performers.add(dto);
        }
        return performers;
    }

    private EmployeePerformanceDTO convertToEmployeePerformanceDTO(User user) {
        return EmployeePerformanceDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getUserType())
                .department(user.getDepartment() != null ? user.getDepartment() : "General")
                .leadsGenerated(leadRepository.countByAssignedTo(user.getId()))
                .leadsConverted(getLeadsConvertedCountForUser(user.getId()))
                .dealsClosed(dealRepository.countClosedWonDealsByUser(user.getId()))
                .revenueGenerated(dealRepository.getUserRevenue(user.getId()))
                .conversionRate(calculateConversionRateForUser(user.getId()))
                .tasksCompleted(taskRepository.countByAssignedToIdAndStatus(user.getId(), Task.TaskStatus.COMPLETED))
                .targetProgress(getMyTargetProgress(user.getId()))
                .build();
    }

    private Long getLeadsConvertedCountForUser(Long userId) {
        // Get count of leads with status "CONVERTED" for this user
        return leadRepository.countByAssignedToAndStatusConverted(userId);
    }

    private Double calculateConversionRateForUser(Long userId) {
        Long total = leadRepository.countByAssignedTo(userId);
        Long converted = getLeadsConvertedCountForUser(userId);
        return calculateConversionRate(total, converted);
    }

    // ============ TEAM PERFORMANCE METHODS ============

    private List<TeamPerformanceDTO> getTeamPerformance() {
        List<Map<String, Object>> results = dealRepository.getTeamPerformance();
        if (results == null) return new ArrayList<>();
        return results.stream()
                .map(r -> TeamPerformanceDTO.builder()
                        .teamName(r.get("userName").toString())
                        .totalMembers(1L)
                        .totalLeads(0L)
                        .totalDeals(((Number) r.get("dealCount")).longValue())
                        .totalRevenue(new BigDecimal(r.get("totalRevenue").toString()))
                        .averageConversionRate(0.0)
                        .build())
                .collect(Collectors.toList());
    }

    private List<TeamPerformanceDTO> getTeamPerformanceByUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return new ArrayList<>();
        List<Map<String, Object>> results = dealRepository.getTeamPerformanceByUsers(userIds);
        if (results == null) return new ArrayList<>();
        return results.stream()
                .map(r -> TeamPerformanceDTO.builder()
                        .teamName(r.get("userName").toString())
                        .totalMembers(1L)
                        .totalLeads(0L)
                        .totalDeals(((Number) r.get("dealCount")).longValue())
                        .totalRevenue(new BigDecimal(r.get("totalRevenue").toString()))
                        .build())
                .collect(Collectors.toList());
    }

    // ============ ACTIVITY METHODS ============

    private List<RecentActivityDTO> getRecentActivities(List<ActivityLog> logs) {
        if (logs == null) return new ArrayList<>();
        return logs.stream().limit(20).map(log -> RecentActivityDTO.builder()
                .id(log.getId())
                .type(log.getActivityType())
                .action(log.getAction())
                .title(log.getEntityName())
                .description(log.getDescription())
                .userName(log.getUser() != null ? log.getUser().getUsername() : "System")
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .activityTime(log.getActivityTime())
                .entityId(log.getEntityId() != null ? String.valueOf(log.getEntityId()) : null)
                .entityName(log.getEntityName())
                .metadata(new HashMap<>())
                .build()
        ).collect(Collectors.toList());
    }

    // ============ CONVERSION RATE METHODS ============

    private Double calculateConversionRate(Long total, Long converted) {
        if (total == null || total == 0) return 0.0;
        if (converted == null) return 0.0;
        return (converted.doubleValue() / total) * 100;
    }

    private Double calculateConversionRateForCompany(Long companyId) {
        Long total = leadRepository.countByCompanyId(companyId);
        Long converted = leadRepository.countByCompanyIdAndStatusConverted(companyId);
        return calculateConversionRate(total, converted);
    }

    private Double calculateConversionRateForTeam(Long teamId) {
        Long total = leadRepository.countByTeamId(teamId);
        Long converted = leadRepository.countByTeamIdAndStatus(teamId, "CONVERTED");
        return calculateConversionRate(total, converted);
    }

    // ============ DATA CONVERSION METHODS ============

    private Map<String, Long> convertToMap(List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) return new LinkedHashMap<>();
        return list.stream().collect(Collectors.toMap(
                m -> {
                    if (m.get("status") != null) return m.get("status").toString();
                    if (m.get("source") != null) return m.get("source").toString();
                    if (m.get("stage") != null) return m.get("stage").toString();
                    if (m.get("priority") != null) return m.get("priority").toString();
                    return "Unknown";
                },
                m -> {
                    Object count = m.get("count");
                    if (count instanceof Number) {
                        return ((Number) count).longValue();
                    }
                    return 0L;
                },
                (v1, v2) -> v1,
                LinkedHashMap::new
        ));
    }

    private Map<String, BigDecimal> convertToMonthlyMap(List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) return new LinkedHashMap<>();
        return list.stream().collect(Collectors.toMap(
                m -> m.get("month").toString(),
                m -> {
                    Object revenue = m.get("revenue");
                    if (revenue instanceof BigDecimal) {
                        return (BigDecimal) revenue;
                    }
                    return new BigDecimal(revenue.toString());
                },
                (v1, v2) -> v1,
                LinkedHashMap::new
        ));
    }

    private Map<String, Long> convertToMonthlyLongMap(List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) return new LinkedHashMap<>();
        return list.stream().collect(Collectors.toMap(
                m -> m.get("month").toString(),
                m -> {
                    Object count = m.get("count");
                    if (count instanceof Number) {
                        return ((Number) count).longValue();
                    }
                    if (m.containsKey("dealsClosed")) {
                        return ((Number) m.get("dealsClosed")).longValue();
                    }
                    return 0L;
                },
                (v1, v2) -> v1,
                LinkedHashMap::new
        ));
    }
}