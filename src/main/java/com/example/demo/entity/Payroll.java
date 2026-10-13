package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.YearMonth;
import com.example.demo.persistence.YearMonthAttributeConverter;

@Entity
@Table(name = "payroll", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "payroll_month"}))
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @jakarta.persistence.Column(name = "payroll_month", nullable = false, length = 7)
    @jakarta.persistence.Convert(converter = YearMonthAttributeConverter.class)
    private YearMonth month;

    @jakarta.persistence.Column(name = "base_salary", precision = 19, scale = 2)
    private BigDecimal baseSalary;

    @jakarta.persistence.Column(name = "overtime_minutes")
    private Long overtimeMinutes;
    @jakarta.persistence.Column(name = "monthly_balance_minutes")
    private Integer monthlyBalanceMinutes;
    @jakarta.persistence.Column(name = "deduction_required")
    private Boolean deductionRequired;

    @jakarta.persistence.Column(name = "leave_days_used", precision = 8, scale = 4)
    private BigDecimal leaveDaysUsed;

    @jakarta.persistence.Column(name = "gross_salary", precision = 19, scale = 2)
    private BigDecimal grossSalary;

    @jakarta.persistence.Column(name = "net_salary", precision = 19, scale = 2)
    private BigDecimal netSalary;

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = month;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public Long getOvertimeMinutes() {
        return overtimeMinutes;
    }

    public void setOvertimeMinutes(Long overtimeMinutes) {
        this.overtimeMinutes = overtimeMinutes;
    }
    public Integer getMonthlyBalanceMinutes() { return monthlyBalanceMinutes; }
    public void setMonthlyBalanceMinutes(Integer value) { monthlyBalanceMinutes = value; }
    public Boolean getDeductionRequired() { return deductionRequired; }
    public void setDeductionRequired(Boolean value) { deductionRequired = value; }

    public BigDecimal getLeaveDaysUsed() {
        return leaveDaysUsed;
    }

    public void setLeaveDaysUsed(BigDecimal leaveDaysUsed) {
        this.leaveDaysUsed = leaveDaysUsed;
    }

    public BigDecimal getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(BigDecimal grossSalary) {
        this.grossSalary = grossSalary;
    }

    public BigDecimal getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(BigDecimal netSalary) {
        this.netSalary = netSalary;
    }
}
