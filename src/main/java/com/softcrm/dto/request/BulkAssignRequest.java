package com.softcrm.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BulkAssignRequest {
    @NotNull(message = "Lead IDs are required")
    private List<Long> leadIds;

    @NotNull(message = "Assigned user ID is required")
    private Long assignedTo;
}