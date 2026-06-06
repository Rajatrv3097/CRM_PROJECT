package com.softcrm.entity;


import com.softcrm.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "login_logs")
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String email;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    private String status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    @PrePersist
    protected void onCreate() {
        loginTime = LocalDateTime.now();
    }
}
