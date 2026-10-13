package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import com.example.demo.entity.Attendance;
import java.time.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AttendanceServiceTest {
    private Attendance day(LocalDate date, String in, String out, Set<LocalDate> holidays) {
        Attendance day = new Attendance();
        day.setWorkDate(date);
        day.setCheckIn(LocalDateTime.of(date, LocalTime.parse(in)));
        if (out != null) day.setCheckOut(LocalDateTime.of(date, LocalTime.parse(out)));
        return AttendanceService.calculate(day, holidays, out == null);
    }

    @Test void normalDayBoundaries() {
        LocalDate monday = LocalDate.of(2026, 9, 14);
        Attendance early = day(monday, "07:30", "18:30", Set.of());
        assertEquals(60, early.getEarlyMinutes());
        assertEquals(-90, early.getBalanceMinutes());
        assertEquals(30, early.getOvertimeMinutes());
        assertEquals(420, early.getWorkingMinutes());
        assertEquals(1.5, early.getOvertimeMultiplier());
        assertEquals(0, day(monday, "06:59", "17:30", Set.of()).getEarlyMinutes());

        Attendance weighted = day(monday, "09:10", "17:30", Set.of());
        assertEquals(10, weighted.getLateMinutes());
        assertEquals(2, weighted.getLateMultiplier());
        assertEquals(20, weighted.getBalanceMinutes());
        assertEquals(0, day(monday, "09:30", "17:30", Set.of()).getMorningMinutes());
        assertEquals(210, day(monday, "09:30", "17:30", Set.of()).getAfternoonMinutes());
        assertEquals(0, day(monday, "10:00", "17:30", Set.of()).getBalanceMinutes());
        assertEquals(10, day(monday, "14:10", "17:30", Set.of()).getBalanceMinutes());
    }

    @Test void missingCheckoutAndOvertimeAreSeparate() {
        LocalDate monday = LocalDate.of(2026, 9, 14);
        Attendance missing = day(monday, "08:30", null, Set.of());
        assertNull(missing.getCheckOut());
        assertEquals(LocalTime.of(17, 30), missing.getEffectiveOut().toLocalTime());
        assertEquals(420, missing.getWorkingMinutes());
        Attendance late = day(monday, "08:45", "18:30", Set.of());
        assertEquals(-15, late.getBalanceMinutes());
        assertEquals(30, late.getOvertimeMinutes());
        assertEquals(60, day(monday, "19:00", "20:00", Set.of()).getOvertimeMinutes());
    }

    @Test void weekendAndHolidayOnlyCreateOvertime() {
        LocalDate saturday = LocalDate.of(2026, 9, 12);
        Attendance weekend = day(saturday, "08:30", "17:30", Set.of());
        assertEquals(420, weekend.getOvertimeMinutes());
        assertEquals(0, weekend.getWorkingMinutes());
        assertEquals(0, weekend.getBalanceMinutes());
        assertEquals(2.0, weekend.getOvertimeMultiplier());
        assertEquals(3.0, day(saturday, "08:30", "17:30", Set.of(saturday)).getOvertimeMultiplier());
    }

    @Test void approvedLeaveStatusIsPreserved() {
        Attendance leave = new Attendance();
        leave.setWorkDate(LocalDate.of(2026, 9, 14));
        leave.setStatus("LEAVE");

        Attendance result = AttendanceService.calculate(leave, Set.of(), true);

        assertEquals("LEAVE", result.getStatus());
        assertNull(result.getCheckIn());
        assertNull(result.getCheckOut());
    }
}
