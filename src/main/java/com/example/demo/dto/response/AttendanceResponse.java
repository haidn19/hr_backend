package com.example.demo.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceResponse {

    private Long id;
    private Long employeeId;
    private LocalDate workDate;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;

    private Integer lateMinutes;
    private Integer latePenaltyMinutes;

    private Integer earlyLeaveMinutes;
    private Integer extraMinutes;

    private Integer workingMinutes;
    private Integer overtimeMinutes;

    private Integer balanceMinutes;

    private Boolean morningWorkLost;
    private Boolean afternoonWorkLost;

    private String status;

    public AttendanceResponse(
            Long id,
            Long employeeId,
            LocalDate workDate,
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            Integer lateMinutes,
            Integer latePenaltyMinutes,
            Integer earlyLeaveMinutes,
            Integer extraMinutes,
            Integer workingMinutes,
            Integer overtimeMinutes,
            Integer balanceMinutes,
            Boolean morningWorkLost,
            Boolean afternoonWorkLost,
            String status
    ) {
        this.id = id;
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.lateMinutes = lateMinutes;
        this.latePenaltyMinutes = latePenaltyMinutes;
        this.earlyLeaveMinutes = earlyLeaveMinutes;
        this.extraMinutes = extraMinutes;
        this.workingMinutes = workingMinutes;
        this.overtimeMinutes = overtimeMinutes;
        this.balanceMinutes = balanceMinutes;
        this.morningWorkLost = morningWorkLost;
        this.afternoonWorkLost = afternoonWorkLost;
        this.status = status;
    }


    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public LocalDateTime getCheckIn() {
        return checkIn;
    }

    public LocalDateTime getCheckOut() {
        return checkOut;
    }

    public Integer getLateMinutes() {
        return lateMinutes;
    }

    public Integer getLatePenaltyMinutes() {
        return latePenaltyMinutes;
    }

    public Integer getEarlyLeaveMinutes() {
        return earlyLeaveMinutes;
    }

    public Integer getExtraMinutes() {
        return extraMinutes;
    }

    public Integer getWorkingMinutes() {
        return workingMinutes;
    }

    public Integer getOvertimeMinutes() {
        return overtimeMinutes;
    }

    public Integer getBalanceMinutes() {
        return balanceMinutes;
    }

    public Boolean getMorningWorkLost() {
        return morningWorkLost;
    }

    public Boolean getAfternoonWorkLost() {
        return afternoonWorkLost;
    }

    public String getStatus() {
        return status;
    }
}