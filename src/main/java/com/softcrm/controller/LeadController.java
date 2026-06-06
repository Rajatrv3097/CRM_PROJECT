package com.softcrm.controller;

import com.softcrm.dto.request.AssignLeadRequest;
import com.softcrm.dto.request.BulkAssignRequest;
import com.softcrm.dto.request.CreateLeadRequest;
import com.softcrm.dto.request.LeadFollowupRequest;
import com.softcrm.dto.request.LeadNoteRequest;
import com.softcrm.dto.request.UpdateLeadRequest;
import com.softcrm.dto.response.ApiResponse;
import com.softcrm.dto.response.LeadResponse;
import com.softcrm.dto.response.UserResponse;
import com.softcrm.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    // ============ Lead CRUD ============
    // SUPER_ADMIN is EXCLUDED - only ADMIN, MANAGER, EMPLOYEE can access leads

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeadResponse>> createLead(@Valid @RequestBody CreateLeadRequest request) {
        try {
            log.info("Create lead request received from user");
            LeadResponse response = leadService.createLead(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Lead created successfully"));
        } catch (Exception e) {
            log.error("Error creating lead: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getAllLeads() {
        try {
            List<LeadResponse> leads = leadService.getAllLeads();
            return ResponseEntity.ok(ApiResponse.success(leads, "Leads fetched successfully"));
        } catch (Exception e) {
            log.error("Error fetching leads: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeadResponse>> getLeadById(@PathVariable Long id) {
        try {
            LeadResponse lead = leadService.getLeadById(id);
            return ResponseEntity.ok(ApiResponse.success(lead, "Lead fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeadResponse>> updateLead(@PathVariable Long id, @Valid @RequestBody UpdateLeadRequest request) {
        try {
            LeadResponse response = leadService.updateLead(id, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Lead updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ Soft Delete (Move to Trash) ============
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeadResponse>> softDeleteLead(@PathVariable Long id) {
        try {
            leadService.deleteLead(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Lead moved to trash successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ PERMANENT DELETE ============
    // Only ADMIN can permanently delete
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> permanentDeleteLead(@PathVariable Long id) {
        try {
            leadService.permanentDeleteLead(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Lead permanently deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ RESTORE FROM TRASH ============
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeadResponse>> restoreLead(@PathVariable Long id) {
        try {
            LeadResponse response = leadService.restoreLead(id);
            return ResponseEntity.ok(ApiResponse.success(response, "Lead restored successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ GET DELETED LEADS (TRASH) ============
    @GetMapping("/trash")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getDeletedLeads() {
        try {
            List<LeadResponse> deletedLeads = leadService.getDeletedLeads();
            return ResponseEntity.ok(ApiResponse.success(deletedLeads, "Trash leads fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ BULK DELETE (Permanent) ============
    @DeleteMapping("/bulk/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> bulkPermanentDelete(@RequestBody List<Long> leadIds) {
        try {
            for (Long id : leadIds) {
                leadService.permanentDeleteLead(id);
            }
            return ResponseEntity.ok(ApiResponse.success(null, leadIds.size() + " leads permanently deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ LEAD ASSIGNMENT APIs ============

    /**
     * Assign a lead to a specific user (Admin/Manager only)
     * POST /api/leads/{id}/assign
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeadResponse>> assignLead(
            @PathVariable Long id,
            @Valid @RequestBody AssignLeadRequest request) {
        try {
            log.info("Assigning lead {} to user {}", id, request.getAssignedTo());
            LeadResponse response = leadService.assignLead(id, request.getAssignedTo());
            return ResponseEntity.ok(ApiResponse.success(response, "Lead assigned successfully"));
        } catch (Exception e) {
            log.error("Error assigning lead: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Reassign lead from one employee to another (Admin/Manager only)
     * POST /api/leads/{id}/reassign?fromUserId={fromUserId}&toUserId={toUserId}
     */
    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeadResponse>> reassignLead(
            @PathVariable Long id,
            @RequestParam Long fromUserId,
            @RequestParam Long toUserId) {
        try {
            log.info("Reassigning lead {} from user {} to user {}", id, fromUserId, toUserId);
            LeadResponse response = leadService.reassignLead(id, fromUserId, toUserId);
            return ResponseEntity.ok(ApiResponse.success(response, "Lead reassigned successfully"));
        } catch (Exception e) {
            log.error("Error reassigning lead: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Unassign a lead (remove assignment) - Admin/Manager only
     * POST /api/leads/{id}/unassign
     */
    @PostMapping("/{id}/unassign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeadResponse>> unassignLead(@PathVariable Long id) {
        try {
            log.info("Unassigning lead {}", id);
            LeadResponse response = leadService.unassignLead(id);
            return ResponseEntity.ok(ApiResponse.success(response, "Lead unassigned successfully"));
        } catch (Exception e) {
            log.error("Error unassigning lead: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get all available employees for assignment (Admin/Manager only)
     * GET /api/leads/available-employees
     */
    @GetMapping("/available-employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAvailableEmployees() {
        try {
            List<UserResponse> employees = leadService.getAvailableEmployeesForAssignment();
            return ResponseEntity.ok(ApiResponse.success(employees, "Available employees fetched successfully"));
        } catch (Exception e) {
            log.error("Error fetching available employees: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get leads assigned to a specific user
     * GET /api/leads/assigned-to/{userId}
     */
    @GetMapping("/assigned-to/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getLeadsAssignedToUser(@PathVariable Long userId) {
        try {
            List<LeadResponse> leads = leadService.getLeadsAssignedToUser(userId);
            return ResponseEntity.ok(ApiResponse.success(leads, "Leads fetched successfully"));
        } catch (Exception e) {
            log.error("Error fetching leads for user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get unassigned leads (Admin/Manager only)
     * GET /api/leads/unassigned
     */
    @GetMapping("/unassigned")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getUnassignedLeads() {
        try {
            List<LeadResponse> leads = leadService.getUnassignedLeads();
            return ResponseEntity.ok(ApiResponse.success(leads, "Unassigned leads fetched successfully"));
        } catch (Exception e) {
            log.error("Error fetching unassigned leads: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Bulk assign leads to an employee (Admin/Manager only)
     * POST /api/leads/bulk-assign
     */
    @PostMapping("/bulk-assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> bulkAssignLeads(@Valid @RequestBody BulkAssignRequest request) {
        try {
            log.info("Bulk assigning {} leads to user {}", request.getLeadIds().size(), request.getAssignedTo());
            leadService.bulkAssignLeads(request.getLeadIds(), request.getAssignedTo());
            return ResponseEntity.ok(ApiResponse.success(null,
                    request.getLeadIds().size() + " leads assigned successfully to user ID: " + request.getAssignedTo()));
        } catch (Exception e) {
            log.error("Error in bulk assignment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ LEAD HISTORY ============
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLeadHistory(@PathVariable Long id) {
        try {
            List<Map<String, Object>> history = leadService.getLeadHistory(id);
            return ResponseEntity.ok(ApiResponse.success(history, "Lead history fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ LEAD NOTES ============
    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLeadNotes(@PathVariable Long id) {
        try {
            List<Map<String, Object>> notes = leadService.getLeadNotes(id);
            return ResponseEntity.ok(ApiResponse.success(notes, "Lead notes fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeadResponse>> addNote(@PathVariable Long id, @Valid @RequestBody LeadNoteRequest request) {
        try {
            LeadResponse response = leadService.addNote(id, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Note added successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeadResponse>> deleteLeadNote(@PathVariable Long id, @PathVariable String noteId) {
        try {
            LeadResponse response = leadService.deleteLeadNote(id, noteId);
            return ResponseEntity.ok(ApiResponse.success(response, "Note deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ Lead Conversion ============
    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeadResponse>> convertToCustomer(@PathVariable Long id) {
        try {
            LeadResponse response = leadService.convertToCustomer(id);
            return ResponseEntity.ok(ApiResponse.success(response, "Lead converted to customer successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ Lead Followups ============
    @PostMapping("/{id}/followups")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeadResponse>> addFollowup(@PathVariable Long id, @Valid @RequestBody LeadFollowupRequest request) {
        try {
            LeadResponse response = leadService.addFollowup(id, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Followup scheduled successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/followups/{index}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeadResponse>> completeFollowup(@PathVariable Long id, @PathVariable Integer index) {
        try {
            LeadResponse response = leadService.completeFollowup(id, index);
            return ResponseEntity.ok(ApiResponse.success(response, "Followup completed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ Lead Scoring ============
    @PostMapping("/{id}/score")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeadResponse>> updateScore(@PathVariable Long id, @RequestParam Integer score) {
        try {
            LeadResponse response = leadService.updateScore(id, score);
            return ResponseEntity.ok(ApiResponse.success(response, "Score updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ Search & Filters ============
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> searchLeads(@RequestParam String keyword) {
        try {
            List<LeadResponse> leads = leadService.searchLeads(keyword);
            return ResponseEntity.ok(ApiResponse.success(leads, "Search results fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getLeadsByStatus(@PathVariable String status) {
        try {
            List<LeadResponse> leads = leadService.getLeadsByStatus(status);
            return ResponseEntity.ok(ApiResponse.success(leads, "Leads by status fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/my-leads")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getMyLeads() {
        try {
            List<LeadResponse> leads = leadService.getMyLeads();
            return ResponseEntity.ok(ApiResponse.success(leads, "My leads fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}