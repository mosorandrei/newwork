package com.newwork.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter
@Setter
public class Employee {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable=false) private String firstName;
    @Column(nullable=false) private String lastName;

    @Version
    private Integer version;

    @Column(nullable=false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate(){ this.updatedAt = Instant.now(); }
}
