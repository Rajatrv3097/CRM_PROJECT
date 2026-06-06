package com.softcrm.entity;

import com.softcrm.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(unique = true, length = 50,nullable = false)
    private String code;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(length = 20)
    private String pincode;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 200)
    private String website;

    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    @Column(name = "pan_number", length = 50)
    private String panNumber;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "subscription_plan", length = 50)
    private String subscriptionPlan = "BASIC";

    @Column(name = "subscription_expiry")
    private LocalDateTime subscriptionExpiry;

    @Column(name = "max_users")
    private Integer maxUsers = 10;

    @Column(name = "max_storage_gb")
    private Integer maxStorageGb = 5;

    @Column(name = "used_storage_gb")
    private Double usedStorageGb = 0.0;

    @Column(length = 1000)
    private String description;

    @Column(name = "industry_type", length = 100)
    private String industryType;

    // ============ CRM MODULES FEATURE FLAGS ============

    @Column(name = "enable_lead_management")
    private Boolean enableLeadManagement = true;

    @Column(name = "enable_customer_management")
    private Boolean enableCustomerManagement = true;

    @Column(name = "enable_sales_pipeline")
    private Boolean enableSalesPipeline = true;

    @Column(name = "enable_marketing_automation")
    private Boolean enableMarketingAutomation = false;

    @Column(name = "enable_support_tickets")
    private Boolean enableSupportTickets = true;

    @Column(name = "enable_hr_module")
    private Boolean enableHRModule = false;

    @Column(name = "enable_inventory")
    private Boolean enableInventory = false;

    @Column(name = "enable_reports")
    private Boolean enableReports = true;

    @Column(name = "enable_email_integration")
    private Boolean enableEmailIntegration = true;

    @Column(name = "enable_whatsapp_integration")
    private Boolean enableWhatsAppIntegration = false;

    @Column(name = "enable_ai_features")
    private Boolean enableAIFeatures = false;

    // ============ Helper Methods ============

    // Check if company has reached user limit
    public boolean hasReachedUserLimit(long currentUserCount) {
        return currentUserCount >= maxUsers;
    }

    // Check if company has storage space
    public boolean hasStorageSpace(double additionalGb) {
        return (usedStorageGb + additionalGb) <= maxStorageGb;
    }

    // Add storage usage
    public void addStorageUsage(double gb) {
        if (hasStorageSpace(gb)) {
            this.usedStorageGb += gb;
        } else {
            throw new RuntimeException("Storage limit exceeded for company: " + name);
        }
    }

    // Check if specific module is enabled for this company
    public boolean isModuleEnabled(String moduleName) {
        switch (moduleName) {
            case "LEAD_MANAGEMENT":
                return enableLeadManagement != null && enableLeadManagement;
            case "CUSTOMER_MANAGEMENT":
                return enableCustomerManagement != null && enableCustomerManagement;
            case "SALES_PIPELINE":
                return enableSalesPipeline != null && enableSalesPipeline;
            case "MARKETING_AUTOMATION":
                return enableMarketingAutomation != null && enableMarketingAutomation;
            case "SUPPORT_TICKETS":
                return enableSupportTickets != null && enableSupportTickets;
            case "HR_MODULE":
                return enableHRModule != null && enableHRModule;
            case "INVENTORY":
                return enableInventory != null && enableInventory;
            case "REPORTS":
                return enableReports != null && enableReports;
            case "EMAIL_INTEGRATION":
                return enableEmailIntegration != null && enableEmailIntegration;
            case "WHATSAPP_INTEGRATION":
                return enableWhatsAppIntegration != null && enableWhatsAppIntegration;
            case "AI_FEATURES":
                return enableAIFeatures != null && enableAIFeatures;
            default:
                return false;
        }
    }

    // Get all enabled modules as list
    public java.util.List<String> getEnabledModules() {
        java.util.List<String> modules = new java.util.ArrayList<>();
        if (isModuleEnabled("LEAD_MANAGEMENT")) modules.add("LEAD_MANAGEMENT");
        if (isModuleEnabled("CUSTOMER_MANAGEMENT")) modules.add("CUSTOMER_MANAGEMENT");
        if (isModuleEnabled("SALES_PIPELINE")) modules.add("SALES_PIPELINE");
        if (isModuleEnabled("MARKETING_AUTOMATION")) modules.add("MARKETING_AUTOMATION");
        if (isModuleEnabled("SUPPORT_TICKETS")) modules.add("SUPPORT_TICKETS");
        if (isModuleEnabled("HR_MODULE")) modules.add("HR_MODULE");
        if (isModuleEnabled("INVENTORY")) modules.add("INVENTORY");
        if (isModuleEnabled("REPORTS")) modules.add("REPORTS");
        if (isModuleEnabled("EMAIL_INTEGRATION")) modules.add("EMAIL_INTEGRATION");
        if (isModuleEnabled("WHATSAPP_INTEGRATION")) modules.add("WHATSAPP_INTEGRATION");
        if (isModuleEnabled("AI_FEATURES")) modules.add("AI_FEATURES");
        return modules;
    }

    // Get subscription plan details
    public String getPlanDisplayName() {
        switch (subscriptionPlan != null ? subscriptionPlan : "BASIC") {
            case "PROFESSIONAL":
                return "Professional";
            case "ENTERPRISE":
                return "Enterprise";
            default:
                return "Basic";
        }
    }

    public double getPlanPrice() {
        switch (subscriptionPlan != null ? subscriptionPlan : "BASIC") {
            case "PROFESSIONAL":
                return 14999.0;
            case "ENTERPRISE":
                return 49999.0;
            default:
                return 4999.0;
        }
    }

    public int getPlanMaxUsers() {
        switch (subscriptionPlan != null ? subscriptionPlan : "BASIC") {
            case "PROFESSIONAL":
                return 50;
            case "ENTERPRISE":
                return 500;
            default:
                return 10;
        }
    }

    public int getPlanMaxStorage() {
        switch (subscriptionPlan != null ? subscriptionPlan : "BASIC") {
            case "PROFESSIONAL":
                return 50;
            case "ENTERPRISE":
                return 500;
            default:
                return 5;
        }
    }
}