package com.example.demo.service;

import com.example.demo.entity.Attendance;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Leave;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.LeaveRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Sends each employee their attendance report for a completed calendar month. */
@Service
public class MonthlyAttendanceReportEmailService {

    private static final Logger log = LoggerFactory.getLogger(MonthlyAttendanceReportEmailService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRepository leaveRepository;
    private final EmailService emailService;
    private final Set<LocalDate> holidays;

    public MonthlyAttendanceReportEmailService(EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository, LeaveRepository leaveRepository,
            EmailService emailService, @Value("${app.holidays:}") String holidayDates) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRepository = leaveRepository;
        this.emailService = emailService;
        this.holidays = HolidayDates.parse(holidayDates);
    }

    /** Gửi báo cáo của tháng được chỉ định chỉ đến địa chỉ email của nhân viên */
    public void sendReports(YearMonth month) {
        LocalDate fromDate = month.atDay(1);
        LocalDate toDate = month.atEndOfMonth();

        for (Employee employee : employeeRepository.findAll()) {
            if (employee.getEmail() == null || employee.getEmail().isBlank()) {
                log.warn("event=monthly_attendance_email_skipped employeeId={} reason=missing_email", employee.getId());
                continue;
            }

            try {
                sendReport(employee, month, fromDate, toDate);
            } catch (RuntimeException exception) {
                // một người không nhận được báo cáo không ngăn cản các nhân viên khác nhận báo cáo
                log.error("event=monthly_attendance_email_failed employeeId={} month={}",
                        employee.getId(), month, exception);
            }
        }
    }

    /** lấy dữ liệu email đúng nhân viên. */
    public void sendReportToEmployeeEmail(String email, YearMonth month) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee email is required");
        }
        Employee employee = employeeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No employee exists with this email address"));
        sendReport(employee, month, month.atDay(1), month.atEndOfMonth());
    }
// lấy dữ liệu  chấm công
    private void sendReport(Employee employee, YearMonth month, LocalDate fromDate, LocalDate toDate) {
        MonthlyAttendanceReport report = buildReport(employee, fromDate, toDate);
        emailService.sendHtmlEmail(employee.getEmail(), "Phieu cham cong thang " + month, renderHtml(report));
        log.info("event=monthly_attendance_email_sent employeeId={} month={} recipient={}",
                employee.getId(), month, employee.getEmail());
    }

    private MonthlyAttendanceReport buildReport(Employee employee, LocalDate fromDate, LocalDate toDate) {
        Map<LocalDate, Attendance> attendancesByDate = attendanceRepository
                .findByEmployeeIdAndWorkDateBetween(employee.getId(), fromDate, toDate).stream()
                // Tháng báo cáo đã kết thúc, do đó có thể chốt lại trường hợp thiếu thông tin trả phòng để tiến hành tính toán.
                .map(attendance -> attendance.getCheckIn() == null ? attendance
                        : AttendanceService.calculate(attendance, holidays, true))
                .collect(Collectors.toMap(Attendance::getWorkDate, Function.identity(), (first, ignored) -> first));

        List<Leave> approvedLeaves = leaveRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employee.getId(), "APPROVED", toDate, fromDate);
        Set<LocalDate> approvedLeaveDates = approvedLeaves.stream()
                .flatMap(leave -> leave.getStartDate().datesUntil(leave.getEndDate().plusDays(1)))
                .filter(date -> !date.isBefore(fromDate) && !date.isAfter(toDate))
                .filter(date -> AttendanceService.isNormalDay(date, holidays))
                .collect(Collectors.toSet());

        List<AttendanceDay> attendanceDays = fromDate.datesUntil(toDate.plusDays(1))
                .filter(date -> AttendanceService.isNormalDay(date, holidays))
                .map(date -> toAttendanceDay(date, attendancesByDate.get(date), approvedLeaveDates.contains(date)))
                .sorted(Comparator.comparing(AttendanceDay::workDate))
                .toList();

        long halfWorkDays = attendanceDays.stream()
                .mapToLong(this::halfWorkDays)
                .sum();
        long leaveDaysUsed = approvedLeaveDates.size();

        return new MonthlyAttendanceReport(employee.getEmployeeName(), YearMonth.from(fromDate), attendanceDays,
                BigDecimal.valueOf(halfWorkDays).divide(BigDecimal.valueOf(2)).stripTrailingZeros(), leaveDaysUsed,
                Math.max(employee.getLeaveDays(), 0));
    }

    private AttendanceDay toAttendanceDay(LocalDate date, Attendance attendance, boolean onApprovedLeave) {
        if (onApprovedLeave || attendance == null || attendance.getCheckIn() == null
                || "LEAVE".equals(attendance.getStatus())) {
            String status = onApprovedLeave || (attendance != null && "LEAVE".equals(attendance.getStatus()))
                    ? "Nghỉ phép" : "Chưa chấm công";
            return new AttendanceDay(date, null, null, 0, 0, status);
        }
        return new AttendanceDay(date, attendance.getCheckIn(), attendance.getCheckOut(),
                valueOrZero(attendance.getMorningMinutes()), valueOrZero(attendance.getAfternoonMinutes()),
                "Đã chấm công");
    }

    private long halfWorkDays(AttendanceDay attendance) {
        long halves = 0;
        if (attendance.morningMinutes() > 0) {
            halves++;
        }
        if (attendance.afternoonMinutes() > 0) {
            halves++;
        }
        return halves;
    }

    private String renderHtml(MonthlyAttendanceReport report) {
        StringBuilder rows = new StringBuilder();
        for (AttendanceDay attendance : report.attendanceDays()) {
            rows.append("<tr><td>").append(DATE_FORMAT.format(attendance.workDate())).append("</td><td>")
                    .append(formatTime(attendance.checkIn())).append("</td><td>")
                    .append(formatTime(attendance.checkOut())).append("</td><td>")
                    .append(escapeHtml(attendance.status())).append("</td></tr>");
        }
        if (rows.isEmpty()) {
            rows.append("<tr><td colspan=\"4\">Khong co du lieu cham cong.</td></tr>");
        }

        return """
                <!doctype html>
                <html><body style="font-family:Arial,sans-serif;color:#1f2937">
                <p>Xin chao %s,</p>
                <p>Day la phieu cham cong thang <strong>%s</strong> cua ban.</p>
                <table border="1" cellpadding="8" cellspacing="0" style="border-collapse:collapse">
                  <thead><tr><th>Ngay lam viec</th><th>Check-in</th><th>Check-out</th><th>Trang thai</th></tr></thead>
                  <tbody>%s</tbody>
                </table>
                <p><strong>Tong so ngay cong:</strong> %s<br>
                <strong>So ngay phep da dung:</strong> %d<br>
                <strong>So ngay phep con lai:</strong> %d</p>
                <p>Trân trọng,<br>Phòng Nhân sự</p>
                </body></html>
                """.formatted(escapeHtml(report.employeeName()), report.month(), rows,
                report.totalWorkDays().toPlainString(), report.leaveDaysUsed(), report.leaveDaysRemaining());
    }

    private String formatTime(LocalDateTime dateTime) {
        return dateTime == null ? "null" : TIME_FORMAT.format(dateTime);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private record AttendanceDay(LocalDate workDate, LocalDateTime checkIn, LocalDateTime checkOut,
            int morningMinutes, int afternoonMinutes, String status) {
    }

    private record MonthlyAttendanceReport(String employeeName, YearMonth month, List<AttendanceDay> attendanceDays,
            BigDecimal totalWorkDays, long leaveDaysUsed, int leaveDaysRemaining) {
    }
}
