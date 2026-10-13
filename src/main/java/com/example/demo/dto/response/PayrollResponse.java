package com.example.demo.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PayrollResponse {

    private Long employeeId;

    private LocalDate fromDate;
    private LocalDate toDate;

    private BigDecimal baseSalary;

    private double paidDays;

    private BigDecimal salary;
    private BigDecimal overtimePay;
    private BigDecimal totalSalary;
    private BigDecimal leaveDaysUsed;
    private int monthlyBalanceMinutes;
    private boolean deductionRequired;

    public PayrollResponse(
            Long employeeId,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal baseSalary,
            double paidDays,
            BigDecimal salary,
            BigDecimal overtimePay,
            BigDecimal totalSalary,
            BigDecimal leaveDaysUsed, int monthlyBalanceMinutes, boolean deductionRequired) {

        this.employeeId = employeeId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.baseSalary = baseSalary;
        this.paidDays = paidDays;
        this.salary = salary;
        this.overtimePay = overtimePay;
        this.totalSalary = totalSalary;
        this.leaveDaysUsed = leaveDaysUsed;
        this.monthlyBalanceMinutes = monthlyBalanceMinutes;
        this.deductionRequired = deductionRequired;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public double getPaidDays() {
        return paidDays;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public BigDecimal getOvertimePay() {
        return overtimePay;
    }

    public BigDecimal getTotalSalary() {
        return totalSalary;
    }

    public BigDecimal getLeaveDaysUsed() {
        return leaveDaysUsed;
    }
    public int getMonthlyBalanceMinutes() { return monthlyBalanceMinutes; }
    public boolean isDeductionRequired() { return deductionRequired; }

}
