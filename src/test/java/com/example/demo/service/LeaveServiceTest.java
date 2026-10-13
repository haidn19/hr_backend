package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.dto.request.LeaveRequest;
import com.example.demo.entity.Attendance;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Leave;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.LeaveRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class LeaveServiceTest {

    @Test void createLeaveCountsOnlyWorkingDays() {
        LeaveRepository leaves = mock(LeaveRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AttendanceRepository attendances = mock(AttendanceRepository.class);
        Employee employee = employee(2);
        LeaveRequest request = new LeaveRequest();
        request.setEmployeeId(1L);
        LocalDate startDate = nextFriday(earliestValidStartDate());
        request.setStartDate(startDate);
        request.setEndDate(startDate.plusDays(3));
        request.setReason("Nghỉ việc gia đình");

        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        when(leaves.save(any(Leave.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(attendances.findByEmployeeIdAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());

        Leave result = new LeaveService(leaves, employees, attendances, "").createLeave(request);

        assertEquals(2, result.getLeaveDays());
    }

    @Test void rejectsLeaveWithoutThreeWorkingDaysNotice() {
        LeaveService service = service(mock(LeaveRepository.class), mock(EmployeeRepository.class),
                mock(AttendanceRepository.class));
        LeaveRequest request = request(nextWorkingDay(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))),
                "Nghỉ việc gia đình");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.createLeave(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Đơn nghỉ phép phải được tạo trước ít nhất 3 ngày làm việc", exception.getReason());
    }

    @Test void rejectsWeekendAsStartOrEndDate() {
        LocalDate saturday = earliestValidStartDate();
        while (saturday.getDayOfWeek().getValue() != 6) {
            saturday = saturday.plusDays(1);
        }
        LeaveService service = service(mock(LeaveRepository.class), mock(EmployeeRepository.class),
                mock(AttendanceRepository.class));
        LeaveRequest request = request(saturday, "Nghỉ việc gia đình");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.createLeave(request));

        assertEquals("Ngày bắt đầu và ngày kết thúc phải là ngày làm việc", exception.getReason());
    }

    @Test void rejectsOverlappingPendingOrApprovedLeave() {
        LeaveRepository leaves = mock(LeaveRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AttendanceRepository attendances = mock(AttendanceRepository.class);
        LocalDate date = earliestValidStartDate();
        LeaveRequest request = request(date, "Nghỉ việc gia đình");
        when(employees.findById(1L)).thenReturn(Optional.of(employee(1)));
        when(leaves.existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                any(), any(), any(), any())).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service(leaves, employees, attendances).createLeave(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Thời gian nghỉ trùng với một đơn đang chờ duyệt hoặc đã được duyệt",
                exception.getReason());
    }

    @Test void rejectsWhenEmployeeAlreadyHasThreePendingLeaves() {
        LeaveRepository leaves = mock(LeaveRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AttendanceRepository attendances = mock(AttendanceRepository.class);
        LocalDate date = earliestValidStartDate();
        LeaveRequest request = request(date, "Nghỉ việc gia đình");
        when(employees.findById(1L)).thenReturn(Optional.of(employee(1)));
        when(leaves.countByEmployeeIdAndStatus(1L, "PENDING")).thenReturn(3L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service(leaves, employees, attendances).createLeave(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Nhân viên đã có tối đa 3 đơn nghỉ phép đang chờ duyệt", exception.getReason());
    }

    @Test void rejectsLeaveForDayWithRecordedAttendance() {
        LeaveRepository leaves = mock(LeaveRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AttendanceRepository attendances = mock(AttendanceRepository.class);
        LocalDate date = earliestValidStartDate();
        LeaveRequest request = request(date, "Nghỉ việc gia đình");
        Attendance attendance = new Attendance();
        attendance.setWorkDate(date);
        attendance.setCheckIn(LocalDateTime.of(date, java.time.LocalTime.of(8, 30)));
        when(employees.findById(1L)).thenReturn(Optional.of(employee(1)));
        when(attendances.findByEmployeeIdAndWorkDateBetween(1L, date, date))
                .thenReturn(List.of(attendance));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service(leaves, employees, attendances).createLeave(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Không thể xin nghỉ cho ngày đã có dữ liệu chấm công", exception.getReason());
    }

    @Test void rejectsInvalidReasonAndMoreThanTenWorkingDays() {
        LeaveRepository leaves = mock(LeaveRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AttendanceRepository attendances = mock(AttendanceRepository.class);
        LocalDate startDate = earliestValidStartDate();

        LeaveRequest shortReason = request(startDate, "Ngắn");
        ResponseStatusException reasonException = assertThrows(
                ResponseStatusException.class,
                () -> service(leaves, employees, attendances).createLeave(shortReason));
        assertEquals("Lý do nghỉ phép phải có từ 10 đến 500 ký tự", reasonException.getReason());

        LeaveRequest tooLong = request(startDate, "Nghỉ việc gia đình");
        tooLong.setEndDate(addWorkingDays(startDate, 10));
        ResponseStatusException durationException = assertThrows(
                ResponseStatusException.class,
                () -> service(leaves, employees, attendances).createLeave(tooLong));
        assertEquals("Mỗi đơn chỉ được nghỉ tối đa 10 ngày làm việc", durationException.getReason());
    }

    @Test void createLeaveRejectsInsufficientLeaveBalance() {
        LeaveRepository leaves = mock(LeaveRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        AttendanceRepository attendances = mock(AttendanceRepository.class);
        Employee employee = employee(1);
        LeaveRequest request = new LeaveRequest();
        request.setEmployeeId(1L);
        LocalDate startDate = earliestValidStartDate();
        request.setStartDate(startDate);
        request.setEndDate(addWorkingDays(startDate, 1));
        request.setReason("Nghỉ việc gia đình");
        when(employees.findById(1L)).thenReturn(Optional.of(employee));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> new LeaveService(leaves, employees, attendances, "").createLeave(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("INSUFFICIENT_LEAVE_BALANCE", exception.getErrorCode());
    }

    private Employee employee(int leaveDays) {
        Employee employee = new Employee("A");
        employee.setId(1L);
        employee.setLeaveDays(leaveDays);
        return employee;
    }

    private LeaveService service(
            LeaveRepository leaves,
            EmployeeRepository employees,
            AttendanceRepository attendances) {
        return new LeaveService(leaves, employees, attendances, "");
    }

    private LeaveRequest request(LocalDate date, String reason) {
        LeaveRequest request = new LeaveRequest();
        request.setEmployeeId(1L);
        request.setStartDate(date);
        request.setEndDate(date);
        request.setReason(reason);
        return request;
    }

    private LocalDate earliestValidStartDate() {
        return addWorkingDays(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")), 3);
    }

    private LocalDate nextWorkingDay(LocalDate date) {
        LocalDate result = date.plusDays(1);
        while (result.getDayOfWeek().getValue() > 5) {
            result = result.plusDays(1);
        }
        return result;
    }

    private LocalDate addWorkingDays(LocalDate date, int workingDays) {
        LocalDate result = date;
        for (int added = 0; added < workingDays;) {
            result = result.plusDays(1);
            if (result.getDayOfWeek().getValue() <= 5) {
                added++;
            }
        }
        return result;
    }

    private LocalDate nextFriday(LocalDate date) {
        LocalDate result = date;
        while (result.getDayOfWeek().getValue() != 5) {
            result = result.plusDays(1);
        }
        return result;
    }
}
