package com.softcrm.controller;

import com.softcrm.dto.request.CreateCustomerRequest;
import com.softcrm.dto.response.ApiResponse;
import com.softcrm.dto.response.CustomerResponse;
import com.softcrm.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ============ Customer CRUD ============

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        try {
            CustomerResponse response = customerService.createCustomer(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Customer created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        try {
            List<CustomerResponse> customers = customerService.getAllCustomers();
            return ResponseEntity.ok(ApiResponse.success(customers, "Customers fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable Long id) {
        try {
            CustomerResponse customer = customerService.getCustomerById(id);
            return ResponseEntity.ok(ApiResponse.success(customer, "Customer fetched successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CreateCustomerRequest request) {
        try {
            CustomerResponse response = customerService.updateCustomer(id, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Customer updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> deleteCustomer(@PathVariable Long id) {
        try {
            customerService.deleteCustomer(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Customer deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ Customer Notes ============

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> addNote(@PathVariable Long id, @RequestParam String note) {
        try {
            CustomerResponse response = customerService.addNote(id, note);
            return ResponseEntity.ok(ApiResponse.success(response, "Note added successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ Customer Contacts ============

    @PostMapping("/{id}/contacts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> addContact(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String designation) {
        try {
            CustomerResponse response = customerService.addContact(id, name, phone, email, designation);
            return ResponseEntity.ok(ApiResponse.success(response, "Contact added successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ Purchase Tracking ============

    @PostMapping("/{id}/purchase")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> recordPurchase(@PathVariable Long id, @RequestParam BigDecimal amount) {
        try {
            CustomerResponse response = customerService.recordPurchase(id, amount);
            return ResponseEntity.ok(ApiResponse.success(response, "Purchase recorded successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ KYC ============

    @PostMapping("/{id}/kyc/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> verifyKYC(@PathVariable Long id, @RequestParam String documentUrl) {
        try {
            CustomerResponse response = customerService.verifyKYC(id, documentUrl);
            return ResponseEntity.ok(ApiResponse.success(response, "KYC verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ Search ============

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> searchCustomers(@RequestParam String keyword) {
        try {
            List<CustomerResponse> customers = customerService.searchCustomers(keyword);
            return ResponseEntity.ok(ApiResponse.success(customers, "Search results fetched"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ Filters ============

    @GetMapping("/by-tier/{tier}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getCustomersByTier(@PathVariable String tier) {
        try {
            Long companyId = customerService.getCurrentUserCompanyIdForFilter();
            List<CustomerResponse> customers = customerService.getCustomersByTier(companyId, tier);
            return ResponseEntity.ok(ApiResponse.success(customers, "Customers by tier fetched"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}