package com.softcrm.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignLeadRequest {
    @NotNull(message = "Assigned user ID is required")
    private Long assignedTo;
}