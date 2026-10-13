package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.demo.dto.response.PayrollResponse;
import com.example.demo.entity.Attendance;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Leave;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.LeaveRepository;
import com.example.demo.repository.PayrollRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PayrollServiceTest {
    @Test void aggregatesMonthlyBalanceAndWeekendOvertimeWithoutUsingOtAsBalance() {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AttendanceRepository attendances = mock(AttendanceRepository.class);
        PayrollRepository payrolls = mock(PayrollRepository.class);
        LeaveRepository leaves = mock(LeaveRepository.class);
        Employee employee = new Employee("A");
        employee.setId(1L);
        employee.setBaseSalary(new BigDecimal("9240000"));
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        when(payrolls.findByEmployeeAndMonth(eq(employee), any())).thenReturn(Optional.empty());
        when(attendances.findByEmployeeIdAndWorkDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of(day(LocalDate.of(2026, 9, 14), "09:10", "18:30"),
                        day(LocalDate.of(2026, 9, 12), "08:30", "17:30")));
        PayrollResponse result = new PayrollService(employees, attendances, payrolls, leaves, "")
                .calculateCurrentPayroll(1L, LocalDate.of(2026, 9, 30));
        assertEquals(-10, result.getMonthlyBalanceMinutes());
        assertFalse(result.isDeductionRequired());
        assertEquals(new BigDecimal("885000.00"), result.getOvertimePay());
        verify(employees, never()).save(any());
    }

    @Test void marksDeductionOnlyAboveSixtyMinutesWithoutInventingMoneyFormula() {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AttendanceRepository attendances = mock(AttendanceRepository.class);
        PayrollRepository payrolls = mock(PayrollRepository.class);
        LeaveRepository leaves = mock(LeaveRepository.class);
        Employee employee = new Employee("A");
        employee.setId(1L);
        employee.setBaseSalary(new BigDecimal("9240000"));
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        when(payrolls.findByEmployeeAndMonth(eq(employee), any())).thenReturn(Optional.empty());
        when(attendances.findByEmployeeIdAndWorkDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of(day(LocalDate.of(2026, 9, 14), "08:30", "16:30")));
        PayrollService service = new PayrollService(employees, attendances, payrolls, leaves, "");
        PayrollResponse atLimit = service.calculateCurrentPayroll(1L, LocalDate.of(2026, 9, 30));
        assertEquals(60, atLimit.getMonthlyBalanceMinutes());
        assertFalse(atLimit.isDeductionRequired());

        when(attendances.findByEmployeeIdAndWorkDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of(day(LocalDate.of(2026, 9, 14), "08:30", "16:29")));
        PayrollResponse aboveLimit = service.calculateCurrentPayroll(1L, LocalDate.of(2026, 9, 30));
        assertEquals(61, aboveLimit.getMonthlyBalanceMinutes());
        assertTrue(aboveLimit.isDeductionRequired());
        assertNull(aboveLimit.getTotalSalary());
    }

    @Test void compensatedLateArrivalDoesNotReduceBaseSalary() {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AttendanceRepository attendances = mock(AttendanceRepository.class);
        PayrollRepository payrolls = mock(PayrollRepository.class);
        LeaveRepository leaves = mock(LeaveRepository.class);
        Employee employee = new Employee("A");
        employee.setId(1L);
        employee.setBaseSalary(new BigDecimal("9240000"));
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        when(payrolls.findByEmployeeAndMonth(eq(employee), any())).thenReturn(Optional.empty());
        when(attendances.findByEmployeeIdAndWorkDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of(day(LocalDate.of(2026, 9, 14), "08:45", "18:00")));
        PayrollResponse result = new PayrollService(employees, attendances, payrolls, leaves, "")
                .calculateCurrentPayroll(1L, LocalDate.of(2026, 9, 30));
        assertEquals(-15, result.getMonthlyBalanceMinutes());
        assertEquals(1.0, result.getPaidDays());
        assertEquals(new BigDecimal("420000.00"), result.getSalary());
        verify(attendances).saveAll(any());
    }

    @Test void approvedLeaveAddsPaidDays() {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AttendanceRepository attendances = mock(AttendanceRepository.class);
        PayrollRepository payrolls = mock(PayrollRepository.class);
        LeaveRepository leaves = mock(LeaveRepository.class);
        Employee employee = new Employee("A");
        employee.setId(1L);
        employee.setBaseSalary(new BigDecimal("9240000"));
        Leave leave = new Leave();
        leave.setEmployeeId(1L);
        leave.setStartDate(LocalDate.of(2026, 9, 14));
        leave.setEndDate(LocalDate.of(2026, 9, 16));
        leave.setLeaveDays(3);
        leave.setStatus("APPROVED");

        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        when(payrolls.findByEmployeeAndMonth(eq(employee), any())).thenReturn(Optional.empty());
        when(attendances.findByEmployeeIdAndWorkDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(leaves.findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(1L), eq("APPROVED"), any(), any())).thenReturn(List.of(leave));

        PayrollResponse result = new PayrollService(employees, attendances, payrolls, leaves, "")
                .calculateCurrentPayroll(1L, LocalDate.of(2026, 9, 30));

        assertEquals(3.0, result.getPaidDays());
        assertEquals(new BigDecimal("1260000.00"), result.getSalary());
        assertEquals(new BigDecimal("3"), result.getLeaveDaysUsed());
    }

    private Attendance day(LocalDate date, String in, String out) {
        Attendance day = new Attendance();
        day.setWorkDate(date);
        day.setCheckIn(LocalDateTime.of(date, LocalTime.parse(in)));
        day.setCheckOut(LocalDateTime.of(date, LocalTime.parse(out)));
        return day;
    }
}
