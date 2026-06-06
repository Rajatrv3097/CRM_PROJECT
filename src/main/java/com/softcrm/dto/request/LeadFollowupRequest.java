package com.softcrm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LeadFollowupRequest {
    @NotBlank(message = "Followup note is required")
    private String note;

    private LocalDateTime followupDate;
}