package com.example.demo.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dto.request.LeaveRequest;
import com.example.demo.entity.Attendance;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Leave;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.LeaveRepository;

@Service
public class LeaveService {
    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);
    private static final int MIN_ADVANCE_WORKING_DAYS = 3;
    private static final int MAX_LEAVE_WORKING_DAYS = 10;
    private static final int MAX_PENDING_LEAVES = 3;
    private static final int MIN_REASON_LENGTH = 10;
    private static final int MAX_REASON_LENGTH = 500;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<String> ACTIVE_LEAVE_STATUSES = List.of("PENDING", "APPROVED");

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final Set<LocalDate> holidays;

    public LeaveService(LeaveRepository leaveRepository, EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository, @Value("${app.holidays:}") String holidayDates) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.holidays = HolidayDates.parse(holidayDates);
    }

    @Transactional(readOnly = true)
    public List<Leave> getAllLeaves() {
        return leaveRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<Leave> getLeavesByEmployeeId(Long employeeId) {
        return leaveRepository.findByEmployeeId(employeeId);
    }

    @Transactional
    public Leave createLeave(LeaveRequest request) {
        if (request == null || request.getEmployeeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã nhân viên không được để trống");
        }
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu và ngày kết thúc không được để trống");
        }
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (startDate.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tạo đơn nghỉ cho ngày trong quá khứ");
        }
        if (!AttendanceService.isNormalDay(startDate, holidays) || !AttendanceService.isNormalDay(endDate, holidays)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu và ngày kết thúc phải là ngày làm việc");
        }
        if (startDate.isBefore(addWorkingDays(today, MIN_ADVANCE_WORKING_DAYS))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn nghỉ phép phải được tạo trước ít nhất 3 ngày làm việc");
        }

        long leaveDays = startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> AttendanceService.isNormalDay(date, holidays))
                .count();
        if (leaveDays == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng nghỉ không có ngày làm việc");
        }
        if (leaveDays > MAX_LEAVE_WORKING_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi đơn chỉ được nghỉ tối đa 10 ngày làm việc");
        }

        String reason = request.getReason() == null ? "" : request.getReason().trim();
        if (reason.length() < MIN_REASON_LENGTH || reason.length() > MAX_REASON_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lý do nghỉ phép phải có từ 10 đến 500 ký tự");
        }

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên"));
        if (leaveRepository.countByEmployeeIdAndStatus(employee.getId(), "PENDING") >= MAX_PENDING_LEAVES) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nhân viên đã có tối đa 3 đơn nghỉ phép đang chờ duyệt");
        }
        if (leaveRepository.existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employee.getId(), ACTIVE_LEAVE_STATUSES, endDate, startDate)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Thời gian nghỉ trùng với một đơn đang chờ duyệt hoặc đã được duyệt");
        }
        if (leaveDays > Math.max(employee.getLeaveDays(), 0)) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_LEAVE_BALANCE", "Số ngày phép còn lại không đủ");
        }
        boolean hasRecordedAttendance = attendanceRepository.findByEmployeeIdAndWorkDateBetween(
                employee.getId(), startDate, endDate).stream()
                .filter(attendance -> AttendanceService.isNormalDay(attendance.getWorkDate(), holidays))
                .anyMatch(attendance -> attendance.getCheckIn() != null || attendance.getCheckOut() != null);
        if (hasRecordedAttendance) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể xin nghỉ cho ngày đã có dữ liệu chấm công");
        }
        Leave leave = new Leave();
        leave.setEmployeeId(employee.getId());
        leave.setEmployeeName(employee.getEmployeeName());
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setLeaveDays((int) leaveDays);
        leave.setReason(reason);
        leave.setStatus("PENDING");
        Leave savedLeave = leaveRepository.save(leave);
        log.info("event=leave_created leaveId={} employeeId={} leaveDays={}",
                savedLeave.getId(), savedLeave.getEmployeeId(), savedLeave.getLeaveDays());
        return savedLeave;
    }

    @Transactional
    public Leave approveLeave(Long leaveId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn nghỉ phép"));
        if (!"PENDING".equals(leave.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn nghỉ phép đã được xử lý");
        }
        Employee employee = employeeRepository.findById(leave.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên"));
        int remainingLeave = Math.max(employee.getLeaveDays(), 0);
        int requestedLeave = leave.getLeaveDays();
        if (requestedLeave > remainingLeave) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_LEAVE_BALANCE", "Số ngày phép còn lại không đủ");
        }
        leave.setStatus("APPROVED");
        updateAttendanceToLeave(leave);
        employee.setLeaveDays(remainingLeave - requestedLeave);
        employeeRepository.save(employee);
        Leave savedLeave = leaveRepository.save(leave);
        log.info("event=leave_approved leaveId={} employeeId={} leaveDays={}",
                savedLeave.getId(), savedLeave.getEmployeeId(), savedLeave.getLeaveDays());
        return savedLeave;
    }

    private LocalDate addWorkingDays(LocalDate date, int workingDays) {
        LocalDate result = date;
        int addedDays = 0;
        while (addedDays < workingDays) {
            result = result.plusDays(1);
            if (AttendanceService.isNormalDay(result, holidays)) {
                addedDays++;
            }
        }
        return result;
    }

    private void updateAttendanceToLeave(Leave leave) {
        LocalDate date = leave.getStartDate();
        while (!date.isAfter(leave.getEndDate())) {
            if (AttendanceService.isNormalDay(date, holidays)) {
                Attendance attendance = attendanceRepository.findByEmployeeIdAndWorkDate(leave.getEmployeeId(), date)
                        .orElseGet(Attendance::new);
                attendance.setEmployeeId(leave.getEmployeeId());
                attendance.setWorkDate(date);
                attendance.setStatus("LEAVE");
                attendanceRepository.save(attendance);
            }
            date = date.plusDays(1);
        }
    }

    public Leave rejectLeave(Long leaveId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn nghỉ phép"));
        if (!"PENDING".equals(leave.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn nghỉ phép đã được xử lý");
        }
        leave.setStatus("REJECTED");
        Leave savedLeave = leaveRepository.save(leave);
        log.info("event=leave_rejected leaveId={} employeeId={}",
                savedLeave.getId(), savedLeave.getEmployeeId());
        return savedLeave;
    }
}
