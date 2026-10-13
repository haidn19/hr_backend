package com.example.demo.controller;

import com.example.demo.service.MonthlyAttendanceReportEmailService;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Admin-only endpoint for testing or re-sending an employee's monthly attendance email. */
@RestController
@RequestMapping("/api/monthly-attendance-reports")
public class MonthlyAttendanceReportEmailController {

    private final MonthlyAttendanceReportEmailService reportEmailService;

    public MonthlyAttendanceReportEmailController(MonthlyAttendanceReportEmailService reportEmailService) {
        this.reportEmailService = reportEmailService;
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> sendToEmployee(
            @RequestParam String email, @RequestParam String month) {
        YearMonth reportMonth;
        try {
            reportMonth = YearMonth.parse(month);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must use YYYY-MM format");
        }

        reportEmailService.sendReportToEmployeeEmail(email, reportMonth);
        return ResponseEntity.ok(Map.of("message", "Attendance report email sent", "email", email,
                "month", reportMonth.toString()));
    }
}
