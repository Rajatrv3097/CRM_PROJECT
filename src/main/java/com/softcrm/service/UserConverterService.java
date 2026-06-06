package com.softcrm.service;

import com.softcrm.dto.request.CreateUserRequest;
import com.softcrm.entity.Role;
import com.softcrm.entity.user.User;
import com.softcrm.enums.UserStatus;
import com.softcrm.repository.RoleRepository;
import com.softcrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserConverterService {

    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public User convertToUser(CreateUserRequest request) {
        User user = new User();

        // Core fields
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUserType(request.getUserType());
        user.setCompanyId(request.getCompanyId());
        user.setStatus(UserStatus.ACTIVE);
        user.setIsEmailVerified(false);
        user.setIsPhoneVerified(false);

        // Common fields
        user.setDepartment(request.getDepartment());
        user.setDesignation(request.getDesignation());

        // Set manager if provided
        if (request.getManagerId() != null) {
            userRepository.findById(request.getManagerId()).ifPresent(user::setManager);
        }

        // Admin/SuperAdmin fields
        if ("ADMIN".equals(request.getUserType()) || "SUPER_ADMIN".equals(request.getUserType())) {
            user.setAccessLevel(request.getAccessLevel());
            user.setCanManageUsers(request.getCanManageUsers());
            user.setCanManageRoles(request.getCanManageRoles());
            user.setAdminCode(request.getAdminCode());
            user.setManagedCompanyId(request.getManagedCompanyId());
            user.setCanManageCompanySettings(request.getCanManageCompanySettings());

            if ("SUPER_ADMIN".equals(request.getUserType())) {
                user.setSystemAccessLevel(request.getSystemAccessLevel());
                user.setCanManageAllTenants(request.getCanManageAllTenants());
                user.setCanAuditLogs(request.getCanAuditLogs());
                user.setCanConfigureSystem(request.getCanConfigureSystem());
            }
        }

        // Employee fields
        if ("EMPLOYEE".equals(request.getUserType())) {
            user.setEmployeeId(request.getEmployeeId());
            user.setDateOfJoining(request.getDateOfJoining());
            user.setSalary(request.getSalary());
            user.setReportingManager(request.getReportingManager());
            user.setBankAccountNumber(request.getBankAccountNumber());
            user.setPanNumber(request.getPanNumber());
        }

        // Manager fields
        if ("MANAGER".equals(request.getUserType())) {
            user.setEmployeeId(request.getEmployeeId());
            user.setDateOfJoining(request.getDateOfJoining());
            user.setSalary(request.getSalary());
            user.setReportingManager(request.getReportingManager());
            user.setTeamName(request.getTeamName());
            user.setTeamSize(request.getTeamSize());
            user.setReportingTo(request.getReportingTo());
            user.setMonthlyTarget(request.getMonthlyTarget());
            user.setAchievedTarget(request.getAchievedTarget());
            user.setCanApproveLeaves(request.getCanApproveLeaves());
        }

        // Customer fields
        if ("CUSTOMER".equals(request.getUserType())) {
            user.setCompanyName(request.getCompanyName());
            user.setGstNumber(request.getGstNumber());
            user.setIndustry(request.getIndustry());
            user.setWebsite(request.getWebsite());
            user.setAddress(request.getAddress());
            user.setCity(request.getCity());
            user.setState(request.getState());
            user.setCountry(request.getCountry());
            user.setPincode(request.getPincode());
            user.setCustomerSource(request.getCustomerSource());
            user.setCustomerTier(request.getCustomerTier() != null ? request.getCustomerTier() : "BRONZE");
            user.setCustomerStatus("ACTIVE");
            user.setTotalPurchases(java.math.BigDecimal.ZERO);
            user.setTotalOrders(0);
            user.setLoyaltyPoints(0);

            if (request.getAssignedTo() != null) {
                userRepository.findById(request.getAssignedTo()).ifPresent(user::setAssignedTo);
            }
        }

        // Assign roles
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = roleRepository.findByNameIn(request.getRoles());
            user.setRoles(roles);
        }

        return user;
    }
}