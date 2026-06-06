package com.softcrm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.util.List;

@Data
public class CreateCustomerRequest {

    @NotBlank(message = "Customer name is required")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    private String alternativePhone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String customerCompanyName;
    private String gstNumber;
    private String panNumber;
    private String industry;
    private String website;
    private String linkedinUrl;
    private String facebookUrl;
    private String twitterUrl;
    private String instagramUrl;
    private String customerType;
    private String customerTier;
    private String customerStatus;
    private List<String> tags;
    private String source;
    private String sourceDetails;
    private Long assignedTo;
}