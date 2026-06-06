package com.softcrm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeadNoteRequest {
    @NotBlank(message = "Note is required")
    private String note;
}