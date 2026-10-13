package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "leaves")
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId; // ID nhân viên

    @Column(name = "employee_name", nullable = false)
    private String employeeName; // Tên nhân viên

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // Ngày bắt đầu nghỉ

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // Ngày kết thúc nghỉ

    @Column(name= "LeaveDays", nullable = false)
    private Integer leaveDays; // Số ngày nghỉ

    // Tương thích với schema MySQL đã từng có cột này. Không dùng trong nghiệp vụ
    // hoặc response API; giá trị 0 giúp các bản ghi mới không vi phạm NOT NULL.
    @Column(name = "unpaid_leave_days", nullable = false)
    private Integer legacyUnpaidLeaveDays = 0;

    @Column(name = "reason", length = 500)
    private String reason; // Lý do

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // Trạng thái

    public Leave() {
    }

    public Leave(Long id, Long employeeId, String employeeName, LocalDate startDate, LocalDate endDate, String reason, String status) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
    }

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

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLeaveDays(int leaveDays) {
        this.leaveDays = leaveDays;
    }

    public Integer getLeaveDays() {
        return leaveDays;
    }

}
