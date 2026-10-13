package com.example.demo.dto.request;

import java.time.LocalDate;
import java.math.BigDecimal;

public record EmployeeRequest(
        String name,
        LocalDate dateOfBirth,
        String phoneNumber,
        String address,
        String email,
        String taxCode,
        Long departmentId,
        BigDecimal baseSalary,
        Integer LeaveDays) {

}
