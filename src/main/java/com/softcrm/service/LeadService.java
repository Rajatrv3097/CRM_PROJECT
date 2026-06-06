package com.softcrm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softcrm.dto.request.CreateLeadRequest;
import com.softcrm.dto.request.LeadFollowupRequest;
import com.softcrm.dto.request.LeadNoteRequest;
import com.softcrm.dto.request.UpdateLeadRequest;
import com.softcrm.dto.response.LeadResponse;
import com.softcrm.dto.response.UserResponse;
import com.softcrm.entity.Customer;
import com.softcrm.entity.Lead;
import com.softcrm.entity.user.User;
import com.softcrm.repository.CustomerRepository;
import com.softcrm.repository.LeadRepository;
import com.softcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    // ============ Helper Methods ============

    private String getCurrentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return "System";
    }

    private User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email).orElse(null);
    }

    private String getCurrentUsername() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getUsername() : "System";
    }

    private String getCurrentUserEmailForHistory() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getEmail() : "system@softcrm.com";
    }

    private Long getCurrentUserCompanyId() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return null;
        if ("SUPER_ADMIN".equals(currentUser.getUserType())) return null;
        return currentUser.getCompanyId();
    }

    private boolean isSuperAdmin() {
        User currentUser = getCurrentUser();
        return currentUser != null && "SUPER_ADMIN".equals(currentUser.getUserType());
    }

    private boolean isAdminOrManager() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;
        String userType = currentUser.getUserType();
        return "ADMIN".equals(userType) || "MANAGER".equals(userType);
    }

    private boolean isEmployee() {
        User currentUser = getCurrentUser();
        return currentUser != null && "EMPLOYEE".equals(currentUser.getUserType());
    }

    private void validateLeadAccess(Long leadId) {
        Long companyId = getCurrentUserCompanyId();
        if (companyId != null) {
            Lead lead = leadRepository.findById(leadId)
                    .orElseThrow(() -> new RuntimeException("Lead not found"));
            if (!lead.getCompanyId().equals(companyId)) {
                throw new RuntimeException("Access denied");
            }
        }
    }

    // ============ JSON Helpers ============

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting to JSON", e);
            return "[]";
        }
    }

    private List<Map<String, Object>> parseJsonArray(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON array: {}", json, e);
            return new ArrayList<>();
        }
    }

    private <T> List<T> fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            if (typeRef.getType().getTypeName().contains("List")) {
                return Collections.singletonList(objectMapper.readValue(json, typeRef));
            }
            return Collections.singletonList(objectMapper.readValue(json, typeRef));
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON", e);
            return new ArrayList<>();
        }
    }

    // ============ Lead History Methods ============

    private void addToHistory(Lead lead, String action, String field, String oldValue, String newValue, String details) {
        List<Map<String, Object>> history = parseJsonArray(lead.getLeadHistory());

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", UUID.randomUUID().toString());
        entry.put("action", action);
        entry.put("field", field);
        entry.put("oldValue", oldValue);
        entry.put("newValue", newValue);
        entry.put("details", details);
        entry.put("timestamp", LocalDateTime.now().toString());
        entry.put("changedBy", getCurrentUsername());
        entry.put("changedByEmail", getCurrentUserEmailForHistory());

        history.add(entry);
        lead.setLeadHistory(toJson(history));
        log.debug("Added history entry for lead {}: {}", lead.getId(), action);
    }

    private void addSimpleHistory(Lead lead, String action, String details) {
        addToHistory(lead, action, null, null, null, details);
    }

    // ============ ASSIGNMENT METHODS ============

    /**
     * Auto-assign lead to creator if creator is EMPLOYEE
     * Admin/Manager can also assign to others
     */
    private User determineAssignedUser(CreateLeadRequest request) {
        User currentUser = getCurrentUser();

        // If assignment is specified in request (by Admin/Manager)
        if (request.getAssignedTo() != null) {
            if (!isAdminOrManager()) {
                throw new RuntimeException("Only Admin or Manager can assign leads to other users");
            }
            User assignedUser = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));

            // Verify user type
            String userType = assignedUser.getUserType();
            if (!"EMPLOYEE".equals(userType) && !"MANAGER".equals(userType)) {
                throw new RuntimeException("Leads can only be assigned to EMPLOYEES or MANAGERS");
            }

            return assignedUser;
        }

        // Auto-assign: If current user is EMPLOYEE, assign to themselves
        if (isEmployee()) {
            log.info("Auto-assigning lead to employee: {}", currentUser.getUsername());
            return currentUser;
        }

        // Admin/Manager creating without assignment - leave unassigned
        return null;
    }

    /**
     * Assign a lead to a specific user (Admin/Manager only)
     */
    @Transactional
    public LeadResponse assignLead(Long leadId, Long assignedToUserId) {
        validateLeadAccess(leadId);

        // Check if current user has permission to assign
        if (!isAdminOrManager()) {
            throw new RuntimeException("Only Admin or Manager can assign leads");
        }

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        User assignedUser = userRepository.findById(assignedToUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user belongs to same company
        Long companyId = getCurrentUserCompanyId();
        if (companyId != null && !assignedUser.getCompanyId().equals(companyId)) {
            throw new RuntimeException("Cannot assign lead to user from different company");
        }

        // Verify user type is EMPLOYEE or MANAGER
        String userType = assignedUser.getUserType();
        if (!"EMPLOYEE".equals(userType) && !"MANAGER".equals(userType)) {
            throw new RuntimeException("Leads can only be assigned to EMPLOYEES or MANAGERS");
        }

        String oldAssignedName = lead.getAssignedTo() != null ?
                lead.getAssignedTo().getUsername() : "Unassigned";
        String newAssignedName = assignedUser.getUsername();

        lead.setAssignedTo(assignedUser);

        addToHistory(lead, "ASSIGNED", "assignedTo", oldAssignedName, newAssignedName,
                "Lead assigned from " + oldAssignedName + " to " + newAssignedName);

        Lead updatedLead = leadRepository.save(lead);
        log.info("Lead {} assigned to user: {} by {}", leadId, assignedUser.getUsername(), getCurrentUsername());

        return convertToResponse(updatedLead);
    }

    /**
     * Reassign lead from one employee to another (Admin/Manager only)
     */
    @Transactional
    public LeadResponse reassignLead(Long leadId, Long fromUserId, Long toUserId) {
        validateLeadAccess(leadId);

        if (!isAdminOrManager()) {
            throw new RuntimeException("Only Admin or Manager can reassign leads");
        }

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        // Verify current assignment matches fromUserId
        if (lead.getAssignedTo() == null || !lead.getAssignedTo().getId().equals(fromUserId)) {
            throw new RuntimeException("Lead is not currently assigned to the specified user");
        }

        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        String fromUserName = lead.getAssignedTo().getUsername();
        String toUserName = toUser.getUsername();

        lead.setAssignedTo(toUser);

        addToHistory(lead, "REASSIGNED", "assignedTo", fromUserName, toUserName,
                "Lead reassigned from " + fromUserName + " to " + toUserName);

        Lead updatedLead = leadRepository.save(lead);
        log.info("Lead {} reassigned from user {} to {} by {}", leadId, fromUserName, toUserName, getCurrentUsername());

        return convertToResponse(updatedLead);
    }
    @Transactional(readOnly = true)
    public List<UserResponse> getAvailableEmployeesForAssignment() {
        if (!isAdminOrManager()) {
            throw new RuntimeException("Only Admin or Manager can view available employees");
        }

        Long companyId = getCurrentUserCompanyId();
        List<User> employees = new ArrayList<>();

        if (companyId != null) {
            // CORRECTED: Use findByCompanyIdAndUserTypeIn instead of findByCompanyIdAndUserTypes
            employees = userRepository.findByCompanyIdAndUserTypeIn(companyId,
                    Arrays.asList("EMPLOYEE", "MANAGER"));
        } else if (isSuperAdmin()) {
            // CORRECTED: Use findByUserTypeIn instead of findByUserType
            employees = userRepository.findByUserTypeIn(Arrays.asList("EMPLOYEE", "MANAGER"));
        }

        log.info("Found {} available employees for assignment", employees.size());

        return employees.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get leads assigned to a specific user
     */
    @Transactional(readOnly = true)
    public List<LeadResponse> getLeadsAssignedToUser(Long userId) {
        Long companyId = getCurrentUserCompanyId();

        // Verify access
        User currentUser = getCurrentUser();
        if (!isAdminOrManager() && !currentUser.getId().equals(userId)) {
            throw new RuntimeException("You can only view your own assigned leads");
        }

        // Verify user belongs to same company (if not super admin)
        if (!isSuperAdmin() && companyId != null) {
            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (!targetUser.getCompanyId().equals(companyId)) {
                throw new RuntimeException("Access denied: User belongs to different company");
            }
        }

        List<Lead> leads = leadRepository.findByAssignedTo(userId);
        return leads.stream()
                .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get unassigned leads (Admin/Manager only)
     */
    @Transactional(readOnly = true)
    public List<LeadResponse> getUnassignedLeads() {
        if (!isAdminOrManager()) {
            throw new RuntimeException("Only Admin or Manager can view unassigned leads");
        }

        Long companyId = getCurrentUserCompanyId();
        List<Lead> leads;

        if (companyId != null) {
            leads = leadRepository.findByCompanyIdAndAssignedToIsNull(companyId);
        } else if (isSuperAdmin()) {
            leads = leadRepository.findByAssignedToIsNull();
        } else {
            leads = new ArrayList<>();
        }

        return leads.stream()
                .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Bulk assign leads to an employee (Admin/Manager only)
     */
    @Transactional
    public void bulkAssignLeads(List<Long> leadIds, Long assignedToUserId) {
        if (!isAdminOrManager()) {
            throw new RuntimeException("Only Admin or Manager can perform bulk assignment");
        }

        User assignedUser = userRepository.findById(assignedToUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        for (Long leadId : leadIds) {
            try {
                assignLead(leadId, assignedToUserId);
            } catch (Exception e) {
                log.error("Failed to assign lead {}: {}", leadId, e.getMessage());
            }
        }

        log.info("Bulk assigned {} leads to user: {} by {}", leadIds.size(), assignedUser.getUsername(), getCurrentUsername());
    }

    /**
     * Unassign a lead (remove assignment)
     */
    @Transactional
    public LeadResponse unassignLead(Long leadId) {
        validateLeadAccess(leadId);

        if (!isAdminOrManager()) {
            throw new RuntimeException("Only Admin or Manager can unassign leads");
        }

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        String oldAssignedName = lead.getAssignedTo() != null ?
                lead.getAssignedTo().getUsername() : "Unassigned";

        lead.setAssignedTo(null);

        addToHistory(lead, "UNASSIGNED", "assignedTo", oldAssignedName, "Unassigned",
                "Lead unassigned from " + oldAssignedName);

        Lead updatedLead = leadRepository.save(lead);
        log.info("Lead {} unassigned by {}", leadId, getCurrentUsername());

        return convertToResponse(updatedLead);
    }

    // ============ Lead CRUD (UPDATED with Role-Based Filtering) ============

    @Transactional
    public LeadResponse createLead(CreateLeadRequest request) {
        log.info("Creating new lead by user: {} with role: {}", getCurrentUsername(), getCurrentUser().getUserType());

        Long companyId = getCurrentUserCompanyId();
        if (companyId == null && !isSuperAdmin()) {
            throw new RuntimeException("No company assigned");
        }

        // Check duplicates
        if (request.getEmail() != null && leadRepository.existsByEmailAndCompanyId(request.getEmail(), companyId)) {
            throw new RuntimeException("Lead with this email already exists");
        }
        if (request.getPhone() != null && leadRepository.existsByPhoneAndCompanyId(request.getPhone(), companyId)) {
            throw new RuntimeException("Lead with this phone already exists");
        }

        Lead lead = new Lead();
        lead.setCompanyId(companyId);
        lead.setName(request.getName());
        lead.setEmail(request.getEmail());
        lead.setPhone(request.getPhone());
        lead.setCompany(request.getCompany());
        lead.setSource(request.getSource() != null ? request.getSource() : "WEBSITE");
        lead.setStatus("NEW");
        lead.setScore(request.getScore() != null ? request.getScore() : 0);
        lead.setDescription(request.getDescription());
        lead.setExpectedRevenue(request.getExpectedRevenue());

        // AUTO-ASSIGNMENT LOGIC
        User assignedUser = determineAssignedUser(request);
        if (assignedUser != null) {
            lead.setAssignedTo(assignedUser);
            log.info("Lead assigned to: {}", assignedUser.getUsername());
        }

        lead.setCreatedBy(getCurrentUsername());

        // Initialize empty JSON arrays
        lead.setFollowups(toJson(new ArrayList<>()));
        lead.setNotes(toJson(new ArrayList<>()));
        lead.setLeadHistory(toJson(new ArrayList<>()));

        // Add to history
        addSimpleHistory(lead, "CREATED", "Lead created with name: " + request.getName());
        if (assignedUser != null) {
            if (isEmployee() && assignedUser.getId().equals(getCurrentUser().getId())) {
                addSimpleHistory(lead, "AUTO_ASSIGNED", "Lead automatically assigned to creator: " + assignedUser.getUsername());
            } else {
                addSimpleHistory(lead, "ASSIGNED", "Lead assigned to: " + assignedUser.getUsername());
            }
        }

        Lead savedLead = leadRepository.save(lead);
        log.info("Lead created with ID: {} by user: {}", savedLead.getId(), getCurrentUsername());

        return convertToResponse(savedLead);
    }

    /**
     * Get all leads based on user role:
     * - ADMIN/MANAGER: See ALL leads in their company
     * - EMPLOYEE: See ONLY leads assigned to them
     */
    @Transactional(readOnly = true)
    public List<LeadResponse> getAllLeads() {
        Long companyId = getCurrentUserCompanyId();
        User currentUser = getCurrentUser();

        List<Lead> leads = new ArrayList<>();

        if (isAdminOrManager()) {
            // ADMIN or MANAGER: See all leads in their company
            log.info("Admin/Manager {} fetching all leads for company: {}", currentUser.getUsername(), companyId);

            if (companyId == null && isSuperAdmin()) {
                leads = leadRepository.findAll();
            } else {
                leads = leadRepository.findAllByCompanyId(companyId);
            }
        } else if (isEmployee()) {
            // EMPLOYEE: See only leads assigned to them
            log.info("Employee {} fetching only assigned leads", currentUser.getUsername());
            leads = leadRepository.findByAssignedTo(currentUser.getId());
        } else {
            // Fallback for other roles
            leads = new ArrayList<>();
        }

        return leads.stream()
                .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LeadResponse getLeadById(Long id) {
        validateLeadAccess(id);
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        // For employees, check if lead is assigned to them
        if (isEmployee()) {
            User currentUser = getCurrentUser();
            if (lead.getAssignedTo() == null || !lead.getAssignedTo().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: Lead not assigned to you");
            }
        }

        return convertToResponse(lead);
    }

    @Transactional
    public LeadResponse updateLead(Long id, UpdateLeadRequest request) {
        validateLeadAccess(id);

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        // For employees, check if lead is assigned to them
        if (isEmployee()) {
            User currentUser = getCurrentUser();
            if (lead.getAssignedTo() == null || !lead.getAssignedTo().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: Cannot update lead not assigned to you");
            }
        }

        // Track old values for history
        String oldName = lead.getName();
        String oldEmail = lead.getEmail();
        String oldPhone = lead.getPhone();
        String oldCompany = lead.getCompany();
        String oldStatus = lead.getStatus();
        String oldSource = lead.getSource();
        String oldDescription = lead.getDescription();
        Integer oldScore = lead.getScore();
        BigDecimal oldExpectedRevenue = lead.getExpectedRevenue();
        Long oldAssignedToId = lead.getAssignedTo() != null ? lead.getAssignedTo().getId() : null;

        // Update fields
        if (request.getName() != null && !request.getName().equals(oldName)) {
            lead.setName(request.getName());
            addToHistory(lead, "UPDATED", "name", oldName, request.getName(), "Name changed");
        }
        if (request.getEmail() != null && !request.getEmail().equals(oldEmail)) {
            lead.setEmail(request.getEmail());
            addToHistory(lead, "UPDATED", "email", oldEmail, request.getEmail(), "Email changed");
        }
        if (request.getPhone() != null && !request.getPhone().equals(oldPhone)) {
            lead.setPhone(request.getPhone());
            addToHistory(lead, "UPDATED", "phone", oldPhone, request.getPhone(), "Phone changed");
        }
        if (request.getCompany() != null && !request.getCompany().equals(oldCompany)) {
            lead.setCompany(request.getCompany());
            addToHistory(lead, "UPDATED", "company", oldCompany, request.getCompany(), "Company name changed");
        }
        if (request.getStatus() != null && !request.getStatus().equals(oldStatus)) {
            lead.setStatus(request.getStatus());
            addToHistory(lead, "STATUS_CHANGED", "status", oldStatus, request.getStatus(), "Status changed from " + oldStatus + " to " + request.getStatus());
        }
        if (request.getSource() != null && !request.getSource().equals(oldSource)) {
            lead.setSource(request.getSource());
            addToHistory(lead, "UPDATED", "source", oldSource, request.getSource(), "Source changed");
        }
        if (request.getDescription() != null && !request.getDescription().equals(oldDescription)) {
            lead.setDescription(request.getDescription());
            addToHistory(lead, "UPDATED", "description", oldDescription, request.getDescription(), "Description updated");
        }
        if (request.getScore() != null && !request.getScore().equals(oldScore)) {
            lead.setScore(request.getScore());
            addToHistory(lead, "SCORE_UPDATED", "score", String.valueOf(oldScore), String.valueOf(request.getScore()), "Score updated");
        }
        if (request.getExpectedRevenue() != null && !request.getExpectedRevenue().equals(oldExpectedRevenue)) {
            lead.setExpectedRevenue(request.getExpectedRevenue());
            addToHistory(lead, "UPDATED", "expectedRevenue",
                    oldExpectedRevenue != null ? oldExpectedRevenue.toString() : "null",
                    request.getExpectedRevenue().toString(), "Expected revenue updated");
        }

        // Handle assignment update (only Admin/Manager can change assignment)
        if (request.getAssignedTo() != null && !request.getAssignedTo().equals(oldAssignedToId)) {
            if (!isAdminOrManager()) {
                throw new RuntimeException("Only Admin or Manager can reassign leads");
            }
            User assignedUser = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));

            String userType = assignedUser.getUserType();
            if (!"EMPLOYEE".equals(userType) && !"MANAGER".equals(userType)) {
                throw new RuntimeException("Leads can only be assigned to EMPLOYEES or MANAGERS");
            }

            String oldAssignedName = lead.getAssignedTo() != null ? lead.getAssignedTo().getUsername() : "Unassigned";
            lead.setAssignedTo(assignedUser);
            addToHistory(lead, "ASSIGNED", "assignedTo", oldAssignedName, assignedUser.getUsername(), "Lead reassigned");
        }

        Lead updatedLead = leadRepository.save(lead);
        log.info("Lead updated: {} by user: {}", updatedLead.getName(), getCurrentUsername());

        return convertToResponse(updatedLead);
    }

    // ============ Other existing methods ============

    @Transactional
    public void permanentDeleteLead(Long leadId) {
        validateLeadAccess(leadId);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));
        leadRepository.delete(lead);
        log.info("Lead permanently deleted: {}", lead.getName());
    }

    @Transactional
    public LeadResponse restoreLead(Long leadId) {
        validateLeadAccess(leadId);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));
        lead.setIsDeleted(false);
        addSimpleHistory(lead, "RESTORED", "Lead restored from trash");
        Lead restored = leadRepository.save(lead);
        log.info("Lead restored: {}", lead.getName());
        return convertToResponse(restored);
    }

    @Transactional(readOnly = true)
    public List<LeadResponse> getDeletedLeads() {
        Long companyId = getCurrentUserCompanyId();
        List<Lead> deletedLeads;

        if (companyId == null && isSuperAdmin()) {
            deletedLeads = leadRepository.findAll().stream()
                    .filter(l -> Boolean.TRUE.equals(l.getIsDeleted()))
                    .collect(Collectors.toList());
        } else {
            deletedLeads = leadRepository.findAllByCompanyId(companyId).stream()
                    .filter(l -> Boolean.TRUE.equals(l.getIsDeleted()))
                    .collect(Collectors.toList());
        }

        return deletedLeads.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLeadHistory(Long leadId) {
        validateLeadAccess(leadId);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));
        return parseJsonArray(lead.getLeadHistory());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLeadNotes(Long leadId) {
        validateLeadAccess(leadId);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));
        return parseJsonArray(lead.getNotes());
    }

    @Transactional
    public LeadResponse deleteLeadNote(Long leadId, String noteId) {
        validateLeadAccess(leadId);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        List<Map<String, Object>> notes = parseJsonArray(lead.getNotes());
        notes.removeIf(note -> noteId.equals(note.get("id")));

        lead.setNotes(toJson(notes));
        addSimpleHistory(lead, "NOTE_DELETED", "Note with ID: " + noteId + " was deleted");

        Lead updated = leadRepository.save(lead);
        return convertToResponse(updated);
    }

    @Transactional
    public void deleteLead(Long id) {
        validateLeadAccess(id);
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found"));
        addSimpleHistory(lead, "DELETED", "Lead moved to trash");
        lead.setIsDeleted(true);
        leadRepository.save(lead);
        log.info("Lead deleted: {}", lead.getName());
    }

    @Transactional
    public LeadResponse convertToCustomer(Long leadId) {
        validateLeadAccess(leadId);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        if (lead.isConverted()) {
            throw new RuntimeException("Lead already converted");
        }

        Customer customer = new Customer();
        customer.setCompanyId(lead.getCompanyId());
        customer.setName(lead.getName());
        customer.setEmail(lead.getEmail());
        customer.setPhone(lead.getPhone());
        customer.setCustomerCompanyName(lead.getCompany());
        customer.setSource(lead.getSource());
        customer.setCustomerType("NEW");
        customer.setCustomerTier("BRONZE");
        customer.setCustomerStatus("ACTIVE");
        customer.setAssignedTo(lead.getAssignedTo());
        customer.setCreatedBy(getCurrentUsername());

        Customer savedCustomer = customerRepository.save(customer);

        String oldStatus = lead.getStatus();
        lead.setStatus("CONVERTED");
        lead.setConvertedCustomer(savedCustomer);
        lead.setConvertedAt(LocalDateTime.now());
        addToHistory(lead, "CONVERTED", "status", oldStatus, "CONVERTED",
                "Lead converted to customer: " + savedCustomer.getName() + " (ID: " + savedCustomer.getId() + ")");

        leadRepository.save(lead);
        log.info("Lead converted to customer: {}", savedCustomer.getName());

        return convertToResponse(lead);
    }

    @Transactional
    public LeadResponse addNote(Long leadId, LeadNoteRequest request) {
        validateLeadAccess(leadId);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        List<Map<String, Object>> notes = parseJsonArray(lead.getNotes());

        Map<String, Object> note = new LinkedHashMap<>();
        note.put("id", UUID.randomUUID().toString());
        note.put("note", request.getNote());
        note.put("createdBy", getCurrentUsername());
        note.put("createdByEmail", getCurrentUserEmailForHistory());
        note.put("date", LocalDateTime.now().toString());
        notes.add(note);

        lead.setNotes(toJson(notes));
        addSimpleHistory(lead, "NOTE_ADDED", "Note added: " + (request.getNote().length() > 50 ?
                request.getNote().substring(0, 50) + "..." : request.getNote()));

        Lead updatedLead = leadRepository.save(lead);
        return convertToResponse(updatedLead);
    }

    @Transactional
    public LeadResponse addFollowup(Long leadId, LeadFollowupRequest request) {
        validateLeadAccess(leadId);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        List<Map<String, Object>> followups = parseJsonArray(lead.getFollowups());

        LocalDateTime followupDate = request.getFollowupDate() != null ?
                request.getFollowupDate() : LocalDateTime.now().plusDays(1);

        Map<String, Object> followup = new LinkedHashMap<>();
        followup.put("id", UUID.randomUUID().toString());
        followup.put("note", request.getNote());
        followup.put("followupDate", followupDate.toString());
        followup.put("status", "PENDING");
        followup.put("createdAt", LocalDateTime.now().toString());
        followup.put("createdBy", getCurrentUsername());
        followups.add(followup);

        lead.setFollowups(toJson(followups));
        addSimpleHistory(lead, "FOLLOWUP_SCHEDULED", "Followup scheduled for " + followupDate + ": " +
                (request.getNote().length() > 50 ? request.getNote().substring(0, 50) + "..." : request.getNote()));

        Lead updatedLead = leadRepository.save(lead);
        return convertToResponse(updatedLead);
    }

    @Transactional
    public LeadResponse completeFollowup(Long leadId, Integer followupIndex) {
        validateLeadAccess(leadId);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        List<Map<String, Object>> followups = parseJsonArray(lead.getFollowups());

        if (followupIndex >= 0 && followupIndex < followups.size()) {
            String oldStatus = (String) followups.get(followupIndex).get("status");
            followups.get(followupIndex).put("status", "COMPLETED");
            followups.get(followupIndex).put("completedAt", LocalDateTime.now().toString());
            followups.get(followupIndex).put("completedBy", getCurrentUsername());
            lead.setFollowups(toJson(followups));
            addToHistory(lead, "FOLLOWUP_COMPLETED", "followup", oldStatus, "COMPLETED",
                    "Followup completed: " + followups.get(followupIndex).get("note"));
        }

        Lead updatedLead = leadRepository.save(lead);
        return convertToResponse(updatedLead);
    }

    @Transactional
    public LeadResponse updateScore(Long leadId, Integer score) {
        validateLeadAccess(leadId);
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        Integer oldScore = lead.getScore();
        lead.setScore(score);
        addToHistory(lead, "SCORE_UPDATED", "score", String.valueOf(oldScore), String.valueOf(score),
                "Lead score updated from " + oldScore + " to " + score);

        Lead updatedLead = leadRepository.save(lead);
        return convertToResponse(updatedLead);
    }

    @Transactional(readOnly = true)
    public List<LeadResponse> getLeadsByStatus(String status) {
        Long companyId = getCurrentUserCompanyId();
        List<Lead> leads;

        if (isAdminOrManager()) {
            // Admin/Manager: Get all leads with status in their company
            leads = leadRepository.findByStatus(companyId, status);
        } else if (isEmployee()) {
            // Employee: Get only their assigned leads with status
            User currentUser = getCurrentUser();
            leads = leadRepository.findByAssignedTo(currentUser.getId()).stream()
                    .filter(l -> l.getStatus().equals(status))
                    .collect(Collectors.toList());
        } else {
            leads = new ArrayList<>();
        }

        return leads.stream()
                .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeadResponse> getMyLeads() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return new ArrayList<>();

        // Only return leads assigned to the current user
        List<Lead> leads = leadRepository.findByAssignedTo(currentUser.getId());
        return leads.stream()
                .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeadResponse> searchLeads(String keyword) {
        Long companyId = getCurrentUserCompanyId();
        User currentUser = getCurrentUser();

        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllLeads();
        }

        List<Lead> leads = new ArrayList<>();

        if (isAdminOrManager()) {
            // Admin/Manager: Search all leads in company
            if (companyId == null && isSuperAdmin()) {
                leads = leadRepository.findAll().stream()
                        .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()))
                        .filter(l -> l.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                                (l.getEmail() != null && l.getEmail().toLowerCase().contains(keyword.toLowerCase())) ||
                                (l.getPhone() != null && l.getPhone().contains(keyword)))
                        .collect(Collectors.toList());
            } else {
                leads = leadRepository.findAllByCompanyId(companyId).stream()
                        .filter(l -> l.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                                (l.getEmail() != null && l.getEmail().toLowerCase().contains(keyword.toLowerCase())) ||
                                (l.getPhone() != null && l.getPhone().contains(keyword)))
                        .collect(Collectors.toList());
            }
        } else if (isEmployee()) {
            // Employee: Search only their assigned leads
            leads = leadRepository.findByAssignedTo(currentUser.getId()).stream()
                    .filter(l -> l.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                            (l.getEmail() != null && l.getEmail().toLowerCase().contains(keyword.toLowerCase())) ||
                            (l.getPhone() != null && l.getPhone().contains(keyword)))
                    .collect(Collectors.toList());
        }

        return leads.stream()
                .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private Lead getLeadEntity(Long id) {
        Long companyId = getCurrentUserCompanyId();
        if (companyId == null && isSuperAdmin()) {
            return leadRepository.findById(id).orElseThrow(() -> new RuntimeException("Lead not found"));
        }
        return leadRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType())
                .department(user.getDepartment())
                .build();
    }

    private LeadResponse convertToResponse(Lead lead) {
        List<Map<String, Object>> followups = parseJsonArray(lead.getFollowups());
        List<Map<String, Object>> notes = parseJsonArray(lead.getNotes());
        List<Map<String, Object>> leadHistory = parseJsonArray(lead.getLeadHistory());

        return LeadResponse.builder()
                .id(lead.getId())
                .companyId(lead.getCompanyId())
                .name(lead.getName())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .company(lead.getCompany())
                .status(lead.getStatus())
                .statusDisplay(lead.getStatusDisplay())
                .source(lead.getSource())
                .sourceDisplay(lead.getSourceDisplay())
                .score(lead.getScore())
                .description(lead.getDescription())
                .assignedToId(lead.getAssignedTo() != null ? lead.getAssignedTo().getId() : null)
                .assignedToName(lead.getAssignedTo() != null ? lead.getAssignedTo().getUsername() : null)
                .followups(followups)
                .notes(notes)
                .leadHistory(leadHistory)
                .convertedCustomerId(lead.getConvertedCustomer() != null ? lead.getConvertedCustomer().getId() : null)
                .convertedCustomerName(lead.getConvertedCustomer() != null ? lead.getConvertedCustomer().getName() : null)
                .convertedAt(lead.getConvertedAt())
                .expectedRevenue(lead.getExpectedRevenue())
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .build();
    }
}