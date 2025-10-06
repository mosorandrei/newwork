package com.newwork.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name="absence_request")
@Getter
@Setter
public class AbsenceRequest {
    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AbsenceType type;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AbsenceStatus status = AbsenceStatus.PENDING;

    @Column(length = 1000)
    private String managerComment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Integer version;

    @PrePersist void prePersist() {
        var now = Instant.now();
        createdAt = now; updatedAt = now;
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}
