package com.softcrm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Set;

@Data
public class CompanyRequest {

    @NotBlank(message = "Company name is required")
    private String name;

//    @NotBlank(message = "Company code is required")
//    private String code;

    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String phone;
    private String email;
    private String website;
    private String gstNumber;
    private String panNumber;

    private String subscriptionPlan = "BASIC";

    @NotNull(message = "Max users is required")
    private Integer maxUsers = 10;

    private Integer maxStorageGb = 5;

    // ✅ NEW: CRM Facilities (Checkboxes)
    private Set<String> enabledModules;  // LEAD_MANAGEMENT, CUSTOMER_MANAGEMENT, SALES_PIPELINE, etc.

    // ✅ NEW: Feature flags
    private Boolean enableLeadManagement = true;
    private Boolean enableCustomerManagement = true;
    private Boolean enableSalesPipeline = true;
    private Boolean enableMarketingAutomation = false;
    private Boolean enableSupportTickets = true;
    private Boolean enableHRModule = false;
    private Boolean enableInventory = false;
    private Boolean enableReports = true;
    private Boolean enableEmailIntegration = true;
    private Boolean enableWhatsAppIntegration = false;
    private Boolean enableAIFeatures = false;
}