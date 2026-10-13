package com.example.demo.service;

import com.example.demo.entity.Attendance;
import com.example.demo.entity.AttendancePunch;
import com.example.demo.repository.AttendancePunchRepository;
import com.example.demo.repository.AttendanceRepository;
import java.time.*;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {
    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);
    private static final String STATUS_LEAVE = "LEAVE";
    // Khung giờ SRS V1: 08:30-12:00, 14:00-17:30; 17:30-18:00 là Bù, sau 18:00 là OT.
    private static final LocalTime EARLY_START = LocalTime.of(7, 0);
    private static final LocalTime START = LocalTime.of(8, 30);
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime MORNING_LOST = LocalTime.of(9, 30);
    private static final LocalTime NOON = LocalTime.NOON;
    private static final LocalTime AFTERNOON = LocalTime.of(14, 0);
    private static final LocalTime END = LocalTime.of(17, 30);
    private static final LocalTime OT_START = LocalTime.of(18, 0);
    private final AttendanceRepository attendanceRepository;
    private final AttendancePunchRepository punchRepository;
    private final Set<LocalDate> holidays;

    public AttendanceService(AttendanceRepository attendanceRepository, AttendancePunchRepository punchRepository,
            @Value("${app.holidays:}") String holidayDates) {
        this.attendanceRepository = attendanceRepository;
        this.punchRepository = punchRepository;
        this.holidays = HolidayDates.parse(holidayDates);
    }

    public static boolean isNormalDay(LocalDate date, Set<LocalDate> holidays) {
        return !holidays.contains(date) && date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
    public static double overtimeRate(LocalDate date, Set<LocalDate> holidays) {
        if (holidays.contains(date)) return 3.0;
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) return 2.0;
        return 1.5;
    }
    private static int minutes(LocalTime start, LocalTime end) {
        return Math.max(0, (int) Duration.between(start, end).toMinutes());
    }
    private static int overlap(LocalTime in, LocalTime out, LocalTime start, LocalTime end) {
        return minutes(in.isAfter(start) ? in : start, out.isBefore(end) ? out : end);
    }

    public static Attendance calculate(Attendance day, Set<LocalDate> holidays, boolean closeMissingCheckout) {
        // Kết quả ngày được tính lại từ IN/OUT; không dùng OT để trừ Bù.
        // closeMissingCheckout chỉ áp dụng cho ngày thường đã qua 17:30, OUT gốc vẫn giữ null.
        if (STATUS_LEAVE.equals(day.getStatus())) return day;
        if (day.getCheckIn() == null) return day;
        LocalTime in = day.getCheckIn().toLocalTime();
        LocalDate date = day.getWorkDate();
        boolean normal = isNormalDay(date, holidays);
        LocalDateTime effective = day.getCheckOut();
        if (effective == null && closeMissingCheckout && normal && !in.isAfter(END)) {
            effective = LocalDateTime.of(date, END);
        }
        day.setEffectiveOut(effective);
        day.setMorningWorkLost(normal && !in.isBefore(MORNING_LOST));
        day.setAfternoonWorkLost(false);
        day.setEarlyMinutes(normal && !in.isBefore(EARLY_START) ? minutes(in, START) : 0);
        int late = normal && in.isAfter(START) && in.isBefore(MORNING_LOST)
                ? (in.isBefore(NINE) ? minutes(START, in) : minutes(NINE, in)) : 0;
        int multiplier = late == 0 ? 0 : in.isBefore(NINE) ? 1 : 2;
        day.setLateMinutes(late);
        day.setLateMultiplier(multiplier);
        day.setLatePenaltyMinutes(late * multiplier);
        day.setOvertimeMultiplier(overtimeRate(date, holidays));
        if (effective == null) {
            day.setStatus("WORKING");
            day.setBalanceMinutes(null);
            day.setMorningMinutes(0);
            day.setAfternoonMinutes(0);
            day.setWorkingMinutes(0);
            day.setOvertimeMinutes(0);
            return day;
        }
        LocalTime out = effective.toLocalTime();
        int morning = normal && !Boolean.TRUE.equals(day.getMorningWorkLost())
                ? overlap(in, out, START, NOON) : 0;
        int afternoon = normal ? overlap(in, out, AFTERNOON, END) : 0;
        day.setMorningMinutes(morning);
        day.setAfternoonMinutes(afternoon);
        day.setWorkingMinutes(morning + afternoon);
        int overtime = normal ? overlap(in, out, OT_START, out)
                : minutes(in, out) - overlap(in, out, NOON, AFTERNOON);
        day.setOvertimeMinutes(Math.max(0, overtime));
        int earlyLeave = normal ? minutes(out, END) - overlap(out, END, NOON, AFTERNOON) : 0;
        if (normal && Boolean.TRUE.equals(day.getMorningWorkLost()) && out.isBefore(NOON)) {
            earlyLeave -= minutes(out, NOON);
        }
        day.setEarlyLeaveMinutes(Math.max(0, earlyLeave));
        int afterWork = normal ? overlap(in, out, END, OT_START) : 0;
        day.setExtraMinutes(day.getEarlyMinutes() + afterWork);
        int afternoonLate = normal && in.isAfter(AFTERNOON) ? overlap(AFTERNOON, in, AFTERNOON, END) : 0;
        day.setBalanceMinutes(normal ? day.getLatePenaltyMinutes() + afternoonLate + day.getEarlyLeaveMinutes()
                - day.getEarlyMinutes() - afterWork : 0);
        day.setStatus(day.getCheckOut() == null ? "DEFAULT_OUT" : "COMPLETED");
        return day;
    }

    @Transactional
    public Attendance checkIn(Long employeeId, LocalDateTime timestamp) {
        Attendance attendance = record(employeeId, timestamp);
        log.info("event=attendance_checked_in employeeId={} workDate={}", employeeId, attendance.getWorkDate());
        return attendance;
    }
    @Transactional
    public Attendance checkOut(Long employeeId, LocalDateTime timestamp) {
        LocalDate date = timestamp.toLocalDate();
        Attendance existing = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, date)
                .orElseThrow(() -> new IllegalArgumentException("Employee has not checked in"));
        if (existing.getCheckIn() == null || !timestamp.isAfter(existing.getCheckIn())) {
            throw new IllegalArgumentException("Checkout must follow checkin");
        }
        Attendance attendance = record(employeeId, timestamp);
        log.info("event=attendance_checked_out employeeId={} workDate={}", employeeId, attendance.getWorkDate());
        return attendance;
    }

    private Attendance record(Long employeeId, LocalDateTime timestamp) {
        // Lưu mọi lần chấm công vào attendance_punch; IN = sớm nhất, OUT = muộn nhất trong ngày.
        // Các mốc ở giữa không tạo ca làm việc riêng theo SRS V1.
        LocalDate date = timestamp.toLocalDate();
        attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, date)
                .filter(attendance -> STATUS_LEAVE.equals(attendance.getStatus()))
                .ifPresent(attendance -> {
                    throw new IllegalStateException("Employee is on approved leave");
                });
        List<AttendancePunch> prior = punchRepository
                .findByEmployeeIdAndPunchedAtGreaterThanEqualAndPunchedAtLessThan(
                        employeeId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        if (prior.isEmpty()) {
            attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, date)
                    .ifPresent(existing -> {
                        if (existing.getCheckIn() != null && !existing.getCheckIn().equals(timestamp)) {
                            punchRepository.save(new AttendancePunch(employeeId, existing.getCheckIn()));
                        }
                        if (existing.getCheckOut() != null && !existing.getCheckOut().equals(timestamp)
                                && !existing.getCheckOut().equals(existing.getCheckIn())) {
                            punchRepository.save(new AttendancePunch(employeeId, existing.getCheckOut()));
                        }
                    });
        }
        punchRepository.save(new AttendancePunch(employeeId, timestamp));
        List<AttendancePunch> punches = punchRepository
                .findByEmployeeIdAndPunchedAtGreaterThanEqualAndPunchedAtLessThan(
                        employeeId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        LocalDateTime first = punches.stream().map(AttendancePunch::getPunchedAt).min(LocalDateTime::compareTo).orElseThrow();
        LocalDateTime last = punches.stream().map(AttendancePunch::getPunchedAt).max(LocalDateTime::compareTo).orElseThrow();
        Attendance day = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, date).orElseGet(Attendance::new);
        day.setEmployeeId(employeeId);
        day.setWorkDate(date);
        day.setCheckIn(first);
        day.setCheckOut(last.equals(first) ? null : last);
        calculate(day, holidays, false);
        return attendanceRepository.save(day);
    }

    @Transactional
    public Attendance daily(Long employeeId, LocalDate date) {
        Attendance day = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, date).orElseThrow();
        return attendanceRepository.save(calculate(day, holidays, shouldClose(date)));
    }
    @Transactional
    public int monthlyBalance(Long employeeId, YearMonth month, LocalDate toDate) {
        // Cộng Bù từng ngày tới toDate. Số dương = thiếu; số âm = dư.
        List<Attendance> days = attendanceRepository.findByEmployeeIdAndWorkDateBetween(employeeId, month.atDay(1), toDate)
                .stream().map(day -> calculate(day, holidays, shouldClose(day.getWorkDate()))).toList();
        attendanceRepository.saveAll(days);
        return days.stream()
                .map(Attendance::getBalanceMinutes).filter(value -> value != null).mapToInt(Integer::intValue).sum();
    }
    @Transactional
    public List<Attendance> history(Long employeeId, LocalDate fromDate, LocalDate toDate) {
        return attendanceRepository.findByEmployeeIdAndWorkDateBetween(employeeId, fromDate, toDate)
                .stream().map(day -> calculate(day, holidays, shouldClose(day.getWorkDate()))).toList();
    }
    private boolean shouldClose(LocalDate date) {
        return date.isBefore(LocalDate.now()) || (date.equals(LocalDate.now()) && !LocalTime.now().isBefore(END));
    }
}
