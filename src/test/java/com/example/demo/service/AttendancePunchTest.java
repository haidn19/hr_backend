package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.demo.entity.Attendance;
import com.example.demo.entity.AttendancePunch;
import com.example.demo.repository.AttendancePunchRepository;
import com.example.demo.repository.AttendanceRepository;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AttendancePunchTest {
    @Test void normalizesEveryPunchToEarliestInAndLatestOut() {
        AttendanceRepository days = mock(AttendanceRepository.class);
        AttendancePunchRepository punches = mock(AttendancePunchRepository.class);
        List<AttendancePunch> stored = new ArrayList<>();
        when(punches.save(any())).thenAnswer(call -> {
            AttendancePunch punch = call.getArgument(0);
            stored.add(punch);
            return punch;
        });
        when(punches.findByEmployeeIdAndPunchedAtGreaterThanEqualAndPunchedAtLessThan(any(), any(), any()))
                .thenAnswer(call -> List.copyOf(stored));
        when(days.findByEmployeeIdAndWorkDate(any(), any())).thenReturn(Optional.empty());
        when(days.save(any())).thenAnswer(call -> call.getArgument(0));
        AttendanceService service = new AttendanceService(days, punches, "");
        LocalDate date = LocalDate.of(2026, 9, 14);
        service.checkIn(1L, LocalDateTime.of(date, LocalTime.of(8, 20)));
        service.checkIn(1L, LocalDateTime.of(date, LocalTime.NOON));
        Attendance result = service.checkIn(1L, LocalDateTime.of(date, LocalTime.of(18, 30)));
        assertEquals(LocalTime.of(8, 20), result.getCheckIn().toLocalTime());
        assertEquals(LocalTime.of(18, 30), result.getCheckOut().toLocalTime());
        assertEquals(30, result.getOvertimeMinutes());
        assertEquals(3, stored.size());
    }

    @Test void retainsBothLegacyTimesWhenFirstNewPunchArrives() {
        AttendanceRepository days = mock(AttendanceRepository.class);
        AttendancePunchRepository punches = mock(AttendancePunchRepository.class);
        List<AttendancePunch> stored = new ArrayList<>();
        when(punches.save(any())).thenAnswer(call -> {
            AttendancePunch punch = call.getArgument(0);
            stored.add(punch);
            return punch;
        });
        when(punches.findByEmployeeIdAndPunchedAtGreaterThanEqualAndPunchedAtLessThan(any(), any(), any()))
                .thenAnswer(call -> List.copyOf(stored));
        LocalDate date = LocalDate.of(2026, 9, 14);
        Attendance legacy = new Attendance();
        legacy.setEmployeeId(1L);
        legacy.setWorkDate(date);
        legacy.setCheckIn(LocalDateTime.of(date, LocalTime.of(8, 30)));
        legacy.setCheckOut(LocalDateTime.of(date, LocalTime.of(17, 30)));
        when(days.findByEmployeeIdAndWorkDate(1L, date)).thenReturn(Optional.of(legacy));
        when(days.save(any())).thenAnswer(call -> call.getArgument(0));
        Attendance result = new AttendanceService(days, punches, "")
                .checkOut(1L, LocalDateTime.of(date, LocalTime.of(18, 30)));
        assertEquals(3, stored.size());
        assertEquals(LocalTime.of(8, 30), result.getCheckIn().toLocalTime());
        assertEquals(LocalTime.of(18, 30), result.getCheckOut().toLocalTime());
        assertEquals(30, result.getOvertimeMinutes());
    }

    @Test void savesDefaultCheckoutForPastNormalDay() {
        AttendanceRepository days = mock(AttendanceRepository.class);
        AttendancePunchRepository punches = mock(AttendancePunchRepository.class);
        LocalDate date = LocalDate.of(2026, 9, 14);
        Attendance attendance = new Attendance();
        attendance.setWorkDate(date);
        attendance.setCheckIn(LocalDateTime.of(date, LocalTime.of(8, 30)));
        when(days.findByEmployeeIdAndWorkDate(1L, date)).thenReturn(Optional.of(attendance));
        when(days.save(any())).thenAnswer(call -> call.getArgument(0));
        Attendance result = new AttendanceService(days, punches, "").daily(1L, date);
        assertEquals("DEFAULT_OUT", result.getStatus());
        assertEquals(LocalTime.of(17, 30), result.getEffectiveOut().toLocalTime());
        verify(days).save(attendance);
    }
}
