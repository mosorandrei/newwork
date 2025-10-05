package com.newwork.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feedback",
        indexes = {
                @Index(name="ix_feedback_employee", columnList = "employee_id"),
                @Index(name="ix_feedback_created",  columnList = "created_at")
        })
@Getter
@Setter
public class Feedback {
    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "author_employee_id", nullable = false)
    private UUID authorEmployeeId;

    @Column(name = "text_original", nullable = false, length = 4000)
    private String textOriginal;

    @Column(name = "text_polished", nullable = false, length = 4000)
    private String textPolished;

    @Column(name = "polish_model", nullable = false)
    private String polishModel;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
