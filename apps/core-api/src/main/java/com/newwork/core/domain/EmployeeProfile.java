package com.newwork.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="employee_profile")
@Getter
@Setter
public class EmployeeProfile {

    @Id
    private UUID employeeId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Version
    private Integer version;

    @Column(columnDefinition = "text")
    private String bio;
    @Column(columnDefinition = "text")
    private String skillsJson;

    // sensitive
    private BigDecimal salary;
    private String ssn;
    private String address;

    // display contact (not login)
    private String contactEmail;
}