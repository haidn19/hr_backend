package com.example.demo.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.entity.Attendance;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Leave;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.LeaveRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MonthlyAttendanceReportEmailServiceTest {

    @Test
    void sendsOneReportUsingTheEmployeeFoundByEmail() {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AttendanceRepository attendances = mock(AttendanceRepository.class);
        LeaveRepository leaves = mock(LeaveRepository.class);
        EmailService emailService = mock(EmailService.class);
        Employee employee = new Employee("Nguyen Van A");
        employee.setId(7L);
        employee.setEmail("employee@example.test");

        when(employees.findByEmailIgnoreCase("employee@example.test")).thenReturn(java.util.Optional.of(employee));
        when(attendances.findByEmployeeIdAndWorkDateBetween(eq(7L), any(), any())).thenReturn(List.of());
        when(leaves.findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(7L), eq("APPROVED"), any(), any())).thenReturn(List.of());

        new MonthlyAttendanceReportEmailService(employees, attendances, leaves, emailService, "")
                .sendReportToEmployeeEmail("employee@example.test", YearMonth.of(2026, 9));

        verify(emailService).sendHtmlEmail(eq("employee@example.test"),
                eq("Phieu cham cong thang 2026-09"), any());
    }

    @Test
    void sendsThePreviousMonthReportToEmployeeEmailWithAttendanceAndLeaveSummary() {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AttendanceRepository attendances = mock(AttendanceRepository.class);
        LeaveRepository leaves = mock(LeaveRepository.class);
        EmailService emailService = mock(EmailService.class);
        Employee employee = new Employee("Nguyen Van A");
        employee.setId(7L);
        employee.setEmail("employee@example.test");
        employee.setLeaveDays(9);
        Attendance attendance = attendance(LocalDate.of(2026, 9, 14), "08:30", "17:30");
        Leave leave = approvedLeave(7L, LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 22));

        when(employees.findAll()).thenReturn(List.of(employee));
        when(attendances.findByEmployeeIdAndWorkDateBetween(eq(7L), any(), any()))
                .thenReturn(List.of(attendance));
        when(leaves.findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(7L), eq("APPROVED"), any(), any())).thenReturn(List.of(leave));

        new MonthlyAttendanceReportEmailService(employees, attendances, leaves, emailService, "")
                .sendReports(YearMonth.of(2026, 9));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtmlEmail(eq("employee@example.test"),
                eq("Phieu cham cong thang 2026-09"), body.capture());
        org.junit.jupiter.api.Assertions.assertTrue(body.getValue().contains("08:30"));
        org.junit.jupiter.api.Assertions.assertTrue(body.getValue().contains("17:30"));
        org.junit.jupiter.api.Assertions.assertTrue(body.getValue()
                .contains("15/09/2026</td><td>null</td><td>null</td><td>Chưa chấm công"));
        org.junit.jupiter.api.Assertions.assertTrue(body.getValue()
                .contains("21/09/2026</td><td>null</td><td>null</td><td>Nghỉ phép"));
        org.junit.jupiter.api.Assertions.assertTrue(body.getValue().contains("Tong so ngay cong:</strong> 1"), body.getValue());
        org.junit.jupiter.api.Assertions.assertTrue(body.getValue().contains("So ngay phep da dung:</strong> 2"));
        org.junit.jupiter.api.Assertions.assertTrue(body.getValue().contains("So ngay phep con lai:</strong> 9"));
    }

    private Attendance attendance(LocalDate date, String checkIn, String checkOut) {
        Attendance attendance = new Attendance();
        attendance.setWorkDate(date);
        attendance.setCheckIn(LocalDateTime.of(date, java.time.LocalTime.parse(checkIn)));
        attendance.setCheckOut(LocalDateTime.of(date, java.time.LocalTime.parse(checkOut)));
        return attendance;
    }

    private Leave approvedLeave(Long employeeId, LocalDate from, LocalDate to) {
        Leave leave = new Leave();
        leave.setEmployeeId(employeeId);
        leave.setStartDate(from);
        leave.setEndDate(to);
        leave.setLeaveDays(2);
        leave.setStatus("APPROVED");
        return leave;
    }
}
