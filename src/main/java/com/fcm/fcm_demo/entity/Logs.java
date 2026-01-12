package com.fcm.fcm_demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fcm_logs")
@Data
public class Logs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "token_count", nullable = false)
    private Integer tokenCount;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "error_code", length = 255)
    private String errorCode;

    @Column(name = "username")
    private String username;

    @Column(name = "extra_data", columnDefinition = "json")
    private String extraData;

    @Column(name = "tracking_id", nullable = false, length = 36)
    private String trackingId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
