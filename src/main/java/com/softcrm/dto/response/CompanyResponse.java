package com.softcrm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {
    private Long id;
    private String name;
    private String code;
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
    private String logoUrl;
    private Boolean isActive;
    private String subscriptionPlan;
    private LocalDateTime subscriptionExpiry;
    private Integer maxUsers;
    private Integer maxStorageGb;
    private Double usedStorageGb;
    private Long totalUsers;
    private LocalDateTime createdAt;
}