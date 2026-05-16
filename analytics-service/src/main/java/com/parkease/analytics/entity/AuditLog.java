package com.parkease.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actionType; // e.g. "LOT_APPROVAL", "USER_SUSPENSION", "MANUAL_CHECKOUT"

    @Column(nullable = false)
    private String performedBy; // email of the admin/manager who performed the action

    @Column(nullable = false)
    private String targetId; // ID of the lot, user, or booking affected

    @Column(length = 500)
    private String details; // Any extra info or feedback

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        timestamp = LocalDateTime.now();
    }
}
