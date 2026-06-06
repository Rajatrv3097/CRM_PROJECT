package com.softcrm.controller;

import com.softcrm.dto.request.CompanyRequest;
import com.softcrm.dto.request.CreateAdminWithCompanyRequest;
import com.softcrm.dto.response.ApiResponse;
import com.softcrm.dto.response.CompanyResponse;
import com.softcrm.dto.response.UserResponse;
import com.softcrm.service.AdminService;
import com.softcrm.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final CompanyService companyService;
    private final AdminService adminService;

    // ============ Company Management ============

    @PostMapping("/companies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(@Valid @RequestBody CompanyRequest request) {
        try {
            CompanyResponse response = companyService.createCompany(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Company created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/companies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> getAllCompanies() {
        try {
            List<CompanyResponse> companies = companyService.getAllCompanies();
            return ResponseEntity.ok(ApiResponse.success(companies, "Companies fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/companies/active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> getActiveCompanies() {
        try {
            List<CompanyResponse> companies = companyService.getActiveCompanies();
            return ResponseEntity.ok(ApiResponse.success(companies, "Active companies fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/companies/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyById(@PathVariable Long id) {
        try {
            CompanyResponse company = companyService.getCompanyById(id);
            return ResponseEntity.ok(ApiResponse.success(company, "Company fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/companies/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
        try {
            CompanyResponse response = companyService.updateCompany(id, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Company updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/companies/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> activateCompany(@PathVariable Long id) {
        try {
            companyService.activateCompany(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Company activated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/companies/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deactivateCompany(@PathVariable Long id) {
        try {
            companyService.deactivateCompany(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Company deactivated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/companies/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteCompany(@PathVariable Long id) {
        try {
            companyService.deleteCompany(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Company deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ Create Admin with Company Assignment ============

    @PostMapping("/create-admin-with-company")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createAdminWithCompany(@Valid @RequestBody CreateAdminWithCompanyRequest request) {
        try {
            UserResponse response = adminService.createAdminWithCompany(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Admin created and assigned to company successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}