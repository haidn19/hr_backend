package com.example.demo.dto.response;

import java.time.LocalDate;
import java.math.BigDecimal;

public record EmployeeResponse(
        Long id,
        String name,
        LocalDate dateOfBirth,
        String phoneNumber,
        String address,
        String email,
        String taxCode,
        DepartmentResponse department,
        BigDecimal baseSalary,
        BigDecimal remainingLeaveDays) {

    public static EmployeeResponse staffView(Long id, String name) {
        return new EmployeeResponse(id, name, null, null, null, null, null, null, null, null);
    }
}