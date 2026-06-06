package com.softcrm.controller;

import com.softcrm.dto.request.CreateUserRequest;
import com.softcrm.dto.response.ApiResponse;
import com.softcrm.dto.response.UserResponse;
import com.softcrm.entity.user.*;
import com.softcrm.service.AdminService;
import com.softcrm.service.UserService;
import com.softcrm.repository.CompanyRepository;
import com.softcrm.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userService.getUserByEmail(email);
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());

        String userType = user.getUserType();
        response.setUserType(userType);

        response.setCompanyId(user.getCompanyId());
        response.setEmailVerified(user.getIsEmailVerified() != null ? user.getIsEmailVerified() : false);
        response.setDepartment(user.getDepartment());

        if (user.getCompanyId() != null) {
            companyRepository.findById(user.getCompanyId()).ifPresent(company ->
                    response.setCompanyName(company.getName()));
        }

        return response;
    }

    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createAdmin(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserResponse response = adminService.createAdmin(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Admin created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/create-manager")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createManager(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserResponse response = adminService.createManager(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Manager created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/create-employee")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<UserResponse>> createEmployee(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserResponse response = adminService.createEmployee(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Employee created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @RequestParam(required = false) String userType) {
        try {
            User currentUser = getCurrentUser();
            String role = currentUser.getUserType();
            List<UserResponse> responses = new ArrayList<>();

            if ("SUPER_ADMIN".equals(role)) {
                // ✅ Use native query to avoid JOIN with admins table
                String sql = "SELECT u.id, u.username, u.email, u.phone, u.first_name, u.last_name, u.user_type, u.company_id, u.department, u.is_email_verified, c.name as company_name " +
                        "FROM users u " +
                        "LEFT JOIN companies c ON u.company_id = c.id " +
                        "WHERE u.user_type = 'ADMIN'";

                List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

                for (Object[] row : results) {
                    UserResponse response = new UserResponse();
                    response.setId(((Number) row[0]).longValue());
                    response.setUsername((String) row[1]);
                    response.setEmail((String) row[2]);
                    response.setPhone((String) row[3]);
                    response.setFirstName((String) row[4]);
                    response.setLastName((String) row[5]);
                    response.setUserType((String) row[6]); // ADMIN
                    response.setCompanyId(row[7] != null ? ((Number) row[7]).longValue() : null);
                    response.setDepartment((String) row[8]);
                    response.setEmailVerified(row[9] != null ? (Boolean) row[9] : false);
                    response.setCompanyName((String) row[10]);
                    responses.add(response);
                }
            }
            else if ("ADMIN".equals(role)) {
                Long adminCompanyId = currentUser.getCompanyId();
                if (adminCompanyId == null) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Admin has no company assigned"));
                }

                String sql;
                if ("MANAGER".equals(userType)) {
                    sql = "SELECT u.id, u.username, u.email, u.phone, u.first_name, u.last_name, u.user_type, u.company_id, u.department, u.is_email_verified, c.name as company_name " +
                            "FROM users u " +
                            "LEFT JOIN companies c ON u.company_id = c.id " +
                            "WHERE u.company_id = :companyId AND u.user_type = 'MANAGER'";
                } else if ("EMPLOYEE".equals(userType)) {
                    sql = "SELECT u.id, u.username, u.email, u.phone, u.first_name, u.last_name, u.user_type, u.company_id, u.department, u.is_email_verified, c.name as company_name " +
                            "FROM users u " +
                            "LEFT JOIN companies c ON u.company_id = c.id " +
                            "WHERE u.company_id = :companyId AND u.user_type = 'EMPLOYEE'";
                } else {
                    sql = "SELECT u.id, u.username, u.email, u.phone, u.first_name, u.last_name, u.user_type, u.company_id, u.department, u.is_email_verified, c.name as company_name " +
                            "FROM users u " +
                            "LEFT JOIN companies c ON u.company_id = c.id " +
                            "WHERE u.company_id = :companyId AND u.user_type IN ('MANAGER', 'EMPLOYEE')";
                }

                List<Object[]> results = entityManager.createNativeQuery(sql)
                        .setParameter("companyId", adminCompanyId)
                        .getResultList();

                for (Object[] row : results) {
                    UserResponse response = new UserResponse();
                    response.setId(((Number) row[0]).longValue());
                    response.setUsername((String) row[1]);
                    response.setEmail((String) row[2]);
                    response.setPhone((String) row[3]);
                    response.setFirstName((String) row[4]);
                    response.setLastName((String) row[5]);
                    response.setUserType((String) row[6]);
                    response.setCompanyId(row[7] != null ? ((Number) row[7]).longValue() : null);
                    response.setDepartment((String) row[8]);
                    response.setEmailVerified(row[9] != null ? (Boolean) row[9] : false);
                    response.setCompanyName((String) row[10]);
                    responses.add(response);
                }
            }
            else if ("MANAGER".equals(role)) {
                // Manager sees their team members
                String sql = "SELECT u.id, u.username, u.email, u.phone, u.first_name, u.last_name, u.user_type, u.company_id, u.department, u.is_email_verified, c.name as company_name " +
                        "FROM users u " +
                        "LEFT JOIN companies c ON u.company_id = c.id " +
                        "WHERE u.manager_id = :managerId AND u.user_type = 'EMPLOYEE'";

                List<Object[]> results = entityManager.createNativeQuery(sql)
                        .setParameter("managerId", currentUser.getId())
                        .getResultList();

                for (Object[] row : results) {
                    UserResponse response = new UserResponse();
                    response.setId(((Number) row[0]).longValue());
                    response.setUsername((String) row[1]);
                    response.setEmail((String) row[2]);
                    response.setPhone((String) row[3]);
                    response.setFirstName((String) row[4]);
                    response.setLastName((String) row[5]);
                    response.setUserType((String) row[6]);
                    response.setCompanyId(row[7] != null ? ((Number) row[7]).longValue() : null);
                    response.setDepartment((String) row[8]);
                    response.setEmailVerified(row[9] != null ? (Boolean) row[9] : false);
                    response.setCompanyName((String) row[10]);
                    responses.add(response);
                }
            }

            return ResponseEntity.ok(ApiResponse.success(responses, "Users fetched successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}