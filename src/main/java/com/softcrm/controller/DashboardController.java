package com.softcrm.controller;

import com.softcrm.dto.response.ApiResponse;
import com.softcrm.dto.response.DashboardStatsDTO;
import com.softcrm.entity.Deal;
import com.softcrm.entity.Lead;
import com.softcrm.entity.Task;
import com.softcrm.entity.Ticket;
import com.softcrm.repository.DealRepository;
import com.softcrm.repository.LeadRepository;
import com.softcrm.repository.TaskRepository;
import com.softcrm.repository.TicketRepository;
import com.softcrm.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    private final TaskRepository taskRepository;

    private final TicketRepository ticketRepository;

    private final LeadRepository leadRepository;

    private final DealRepository dealRepository;

    private String getCurrentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    @GetMapping("/super-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getSuperAdminDashboard() {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    dashboardService.getSuperAdminDashboard(),
                    "Super Admin dashboard loaded"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getAdminDashboard() {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    dashboardService.getAdminDashboard(getCurrentUserEmail()),
                    "Admin dashboard loaded"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getManagerDashboard() {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    dashboardService.getManagerDashboard(getCurrentUserEmail()),
                    "Manager dashboard loaded"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/employee")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getEmployeeDashboard() {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    dashboardService.getEmployeeDashboard(getCurrentUserEmail()),
                    "Employee dashboard loaded"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/my-dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getMyDashboard() {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    dashboardService.getDashboardByUserRole(getCurrentUserEmail()),
                    "Dashboard loaded"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

}