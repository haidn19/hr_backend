package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "work_date"}))
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id")
    private Long employeeId;
    @Column(name = "work_date")
    private LocalDate workDate;
    @Column(name = "check_in")
    private LocalDateTime checkIn;
    @Column(name = "check_out")
    private LocalDateTime checkOut;

    @Column(name = "effective_out")
    private LocalDateTime effectiveOut;
    @Column(name = "morning_minutes")
    private Integer morningMinutes = 0;
    @Column(name = "afternoon_minutes")
    private Integer afternoonMinutes = 0;
    @Column(name = "early_minutes")
    private Integer earlyMinutes = 0;
    @Column(name = "late_multiplier")
    private Integer lateMultiplier = 0;
    @Column(name = "overtime_multiplier")
    private Double overtimeMultiplier = 0.0;

    // Số phút đi muộn thực tế
    @Column(name = "late_minutes")
    private Integer lateMinutes = 0;

    // Số phút đi muộn sau khi áp dụng hệ số x1, x2
    @Column(name = "late_penalty_minutes")
    private Integer latePenaltyMinutes = 0;

    // Số phút về sớm
    @Column(name = "early_leave_minutes")
    private Integer earlyLeaveMinutes = 0;

    // Số phút thừa dùng để bù
    @Column(name = "extra_minutes")
    private Integer extraMinutes = 0;

    // Tổng thời gian làm việc thực tế
    @Column(name = "working_minutes")
    private Integer workingMinutes = 0;

    // Thời gian làm thêm giờ
    @Column(name = "overtime_minutes")
    private Integer overtimeMinutes = 0;

    // Dương = thiếu
    // Âm = thừa
    @Column(name = "balance_minutes")
    private Integer balanceMinutes;

    // true = mất công sáng
    // false = không mất công sáng
    @Column(name = "morning_work_lost")
    private Boolean morningWorkLost = false;

    // true = mất công chiều do check-in quá 30 phút sau 13:00
    @Column(name = "afternoon_work_lost")
    private Boolean afternoonWorkLost = false;

    // WORKING, COMPLETED, ABSENT...
    @Column(name = "status")
    private String status;


    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }
    public LocalDate getWorkDate() {
        return workDate;
    }
    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }
    public LocalDateTime getCheckIn() {
        return checkIn;
    }
    public void setCheckIn(LocalDateTime checkIn) {
        this.checkIn = checkIn;
    }
    public LocalDateTime getCheckOut() {
        return checkOut;
    }
    public void setCheckOut(LocalDateTime checkOut) {
        this.checkOut = checkOut;
    }
    public LocalDateTime getEffectiveOut() { return effectiveOut; }
    public void setEffectiveOut(LocalDateTime value) { effectiveOut = value; }
    public Integer getMorningMinutes() { return morningMinutes; }
    public void setMorningMinutes(Integer value) { morningMinutes = value; }
    public Integer getAfternoonMinutes() { return afternoonMinutes; }
    public void setAfternoonMinutes(Integer value) { afternoonMinutes = value; }
    public Integer getEarlyMinutes() { return earlyMinutes; }
    public void setEarlyMinutes(Integer value) { earlyMinutes = value; }
    public Integer getLateMultiplier() { return lateMultiplier; }
    public void setLateMultiplier(Integer value) { lateMultiplier = value; }
    public Double getOvertimeMultiplier() { return overtimeMultiplier; }
    public void setOvertimeMultiplier(Double value) { overtimeMultiplier = value; }
    public Integer getLateMinutes() {
        return lateMinutes;
    }
    public void setLateMinutes(Integer lateMinutes) {
        this.lateMinutes = lateMinutes;
    }
    public Integer getLatePenaltyMinutes() {
        return latePenaltyMinutes;
    }
    public void setLatePenaltyMinutes(Integer latePenaltyMinutes) {
        this.latePenaltyMinutes = latePenaltyMinutes;
    }
    public Integer getEarlyLeaveMinutes() {
        return earlyLeaveMinutes;
    }
    public void setEarlyLeaveMinutes(Integer earlyLeaveMinutes) {
        this.earlyLeaveMinutes = earlyLeaveMinutes;
    }
    public Integer getExtraMinutes() {
        return extraMinutes;
    }
    public void setExtraMinutes(Integer extraMinutes) {
        this.extraMinutes = extraMinutes;
    }
    public Integer getWorkingMinutes() {
        return workingMinutes;
    }
    public void setWorkingMinutes(Integer workingMinutes) {
        this.workingMinutes = workingMinutes;
    }
    public Integer getOvertimeMinutes() {
        return overtimeMinutes;
    }
    public void setOvertimeMinutes(Integer overtimeMinutes) {
        this.overtimeMinutes = overtimeMinutes;
    }
    public Integer getBalanceMinutes() {
        return balanceMinutes;
    }
    public void setBalanceMinutes(Integer balanceMinutes) {
        this.balanceMinutes = balanceMinutes;
    }
    public Boolean getMorningWorkLost() {
        return morningWorkLost;
    }
    public void setMorningWorkLost(Boolean morningWorkLost) {
        this.morningWorkLost = morningWorkLost;
    }
    public Boolean getAfternoonWorkLost() {
        return afternoonWorkLost;
    }
    public void setAfternoonWorkLost(Boolean afternoonWorkLost) {
        this.afternoonWorkLost = afternoonWorkLost;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
