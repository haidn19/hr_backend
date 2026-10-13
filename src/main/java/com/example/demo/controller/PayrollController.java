package com.example.demo.controller;

import com.example.demo.dto.response.PayrollResponse;
import com.example.demo.service.PayrollService;
import com.example.demo.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;
    private final CurrentUserService currentUserService;

    public PayrollController(
            PayrollService payrollService,
            CurrentUserService currentUserService) {
                this.payrollService = payrollService;
                this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public PayrollResponse getMyPayroll(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {
        LocalDate toDate = month == null || year == null
                ? LocalDate.now()
                : java.time.YearMonth.of(year, month).atEndOfMonth();
        return payrollService.calculateCurrentPayroll(
                currentUserService.getCurrentEmployee(authentication).getId(), toDate);
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    // Ví dụ: GET /api/payroll/1?toDate=2026-09-30
    // Endpoint hiện tại vừa tính vừa lưu payroll; xem ghi chú ở PayrollService trước khi dùng số tiền.
    public PayrollResponse getCurrentPayroll(
            @PathVariable Long employeeId,
            @RequestParam LocalDate toDate) {

        return payrollService
            .calculateCurrentPayroll(
                employeeId,
                toDate
                );
    }
}
