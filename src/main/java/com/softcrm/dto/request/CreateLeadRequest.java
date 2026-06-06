package com.softcrm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateLeadRequest {

    @NotBlank(message = "Lead name is required")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be exactly 10 digits")
    private String phone;

    private String company;

    private String source;  // WEBSITE, FACEBOOK_ADS, GOOGLE_ADS, INSTAGRAM, REFERRAL, etc.

    private String description;

    // Assignment field - can be set by ADMIN or MANAGER
    // If EMPLOYEE creates lead, this will be auto-assigned to themselves
    private Long assignedTo;

    private BigDecimal expectedRevenue;

    // Optional: Add status field (default will be "NEW" in service)
    private String status;

    // Optional: Add score field (default will be 0 in service)
    private Integer score;
}