package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_punch", indexes = @Index(name = "idx_punch_employee_time", columnList = "employee_id,punched_at"))
public class AttendancePunch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;
    @Column(name = "punched_at", nullable = false)
    private LocalDateTime punchedAt;

    protected AttendancePunch() {}
    public AttendancePunch(Long employeeId, LocalDateTime punchedAt) {
        this.employeeId = employeeId;
        this.punchedAt = punchedAt;
    }
    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public LocalDateTime getPunchedAt() { return punchedAt; }
}
