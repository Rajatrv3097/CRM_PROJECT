package com.softcrm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateLeadRequest {

    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be exactly 10 digits")
    private String phone;

    private String company;

    private String status;  // NEW, CONTACTED, QUALIFIED, PROPOSAL_SENT, NEGOTIATION, CONVERTED, LOST

    private String source;  // WEBSITE, FACEBOOK_ADS, GOOGLE_ADS, INSTAGRAM, REFERRAL, etc.

    private String description;

    // Assignment field - can be updated by ADMIN or MANAGER only
    private Long assignedTo;

    private BigDecimal expectedRevenue;

    private Integer score;
}