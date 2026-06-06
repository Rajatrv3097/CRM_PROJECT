// Create new file: com/softcrm/dto/request/UpdateProfileRequest.java
package com.softcrm.dto.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String department;
    private String designation;

    // Customer specific
    private String companyName;
    private String gstNumber;
    private String industry;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String website;
}

