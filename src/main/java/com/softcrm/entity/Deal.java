package com.softcrm.entity;

import com.softcrm.entity.base.BaseEntity;
import com.softcrm.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "deals")
public class Deal extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;  // Changed from Customer to User

    @ManyToOne
    @JoinColumn(name = "lead_id")
    private Lead lead;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private DealStage stage = DealStage.PROSPECTING;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    private LocalDateTime expectedCloseDate;
    private LocalDateTime closedDate;

    private String competitor;
    private String winLossReason;

    @Enumerated(EnumType.STRING)
    private DealStatus status = DealStatus.ACTIVE;

    public enum DealStage {
        PROSPECTING, QUALIFICATION, PROPOSAL, NEGOTIATION, CLOSED_WON, CLOSED_LOST
    }

    public enum DealStatus {
        ACTIVE, WON, LOST, ON_HOLD
    }
}