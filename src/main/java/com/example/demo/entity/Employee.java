package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "employees")
public class Employee {

    /**
     * Khóa chính của nhân viên.
     * Giá trị được tự động tăng bởi database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "employee_name")
    private String employeeName;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String address;
    private String email;
    private String taxCode;

    
    /**
     * Phòng ban mà nhân viên đang làm việc.
     * Quan hệ nhiều nhân viên thuộc về một phòng ban.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "department")
    private String legacyDepartment;

    public Employee() {
    }

    public Employee(String employeeName) {
        this.employeeName = employeeName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getLegacyDepartment() {
        return legacyDepartment;
    }

    public void setLegacyDepartment(String legacyDepartment) {
        this.legacyDepartment = legacyDepartment;
    }



    // payroll

    @Column(name = "base_salary", precision = 19, scale = 2)
    private BigDecimal baseSalary;

    // số ngày phép còn lại
    @Column(name = "leave_days", precision = 8, scale = 2)
    private Integer LeaveDays;


    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public int getLeaveDays() {
        return LeaveDays == null ? 0 : LeaveDays;
    }

    public void setLeaveDays(int LeaveDays) {
        this.LeaveDays = LeaveDays;
    }
}
