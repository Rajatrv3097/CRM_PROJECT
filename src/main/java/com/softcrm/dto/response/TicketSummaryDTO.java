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
    public class TicketSummaryDTO {
        private Long id;
        private String ticketNumber;
        private String title;
        private String status;
        private String priority;
        private String customerName;
        private LocalDateTime createdAt;
        private LocalDateTime resolvedAt;
        private String assignedTo;
    }