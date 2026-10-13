package com.example.demo.controller;

import com.example.demo.entity.Attendance;
import com.example.demo.service.AttendanceService;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

        @RestController
        @RequestMapping("/api/attendance")
        public class AttendanceController {

        private final AttendanceService attendanceService;
        private final com.example.demo.service.CurrentUserService currentUserService;

        // Gọi bằng JWT sau khi đăng nhập. employeeId là ID của nhân viên trong bảng employees.
        // Không truyền giờ từ client: server dùng thời gian hiện tại của máy đang chạy backend.

        public AttendanceController(
                AttendanceService attendanceService,
                com.example.demo.service.CurrentUserService currentUserService
        ) {
                this.attendanceService =
                        attendanceService;
                this.currentUserService = currentUserService;
        }

        @PostMapping("/check-in")
        @PreAuthorize("hasRole('EMPLOYEE')")
        public ResponseEntity<Attendance> checkInMe(Authentication authentication) {
                return ResponseEntity.ok(attendanceService.checkIn(
                        currentUserService.getCurrentEmployee(authentication).getId(), LocalDateTime.now()));
        }

        @PostMapping("/check-out")
        @PreAuthorize("hasRole('EMPLOYEE')")
        public ResponseEntity<Attendance> checkOutMe(Authentication authentication) {
                return ResponseEntity.ok(attendanceService.checkOut(
                        currentUserService.getCurrentEmployee(authentication).getId(), LocalDateTime.now()));
        }

        @GetMapping("/me")
        @PreAuthorize("hasRole('EMPLOYEE')")
        public List<Attendance> myAttendance(
                @RequestParam(required = false) LocalDate from,
                @RequestParam(required = false) LocalDate to,
                Authentication authentication) {
                Long employeeId = currentUserService.getCurrentEmployee(authentication).getId();
                LocalDate end = to == null ? LocalDate.now() : to;
                LocalDate start = from == null ? end.withDayOfMonth(1) : from;
                return attendanceService.history(employeeId, start, end);
        }

        @PostMapping("/check-in/{employeeId}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Attendance> checkIn(
                @PathVariable Long employeeId
        ) {

                Attendance result =
                        attendanceService.checkIn(
                                employeeId,
                                LocalDateTime.now()
                        );

                return ResponseEntity.ok(result);
        }

        @PostMapping("/check-out/{employeeId}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Attendance> checkOut(
                @PathVariable Long employeeId
        ) {

                Attendance result =
                        attendanceService.checkOut(
                                employeeId,
                                LocalDateTime.now()
                        );
                return ResponseEntity.ok(result);
        }
        @GetMapping("/{employeeId}/daily")
        @PreAuthorize("hasRole('ADMIN')")
        // Ví dụ: GET /api/attendance/1/daily?date=2026-09-14
        // checkOut có thể null khi quên chấm ra; effectiveOut là giờ hệ thống dùng để tính.
        public Attendance daily(@PathVariable Long employeeId, @RequestParam LocalDate date) {
                return attendanceService.daily(employeeId, date);
        }

        @GetMapping("/{employeeId}/monthly-balance")
        @PreAuthorize("hasRole('ADMIN')")
        // Ví dụ: GET /api/attendance/1/monthly-balance?month=2026-09&toDate=2026-09-30
        // Kết quả tính bằng phút: dương là thiếu, âm là dư; OT không bù vào số này.
        public int monthlyBalance(@PathVariable Long employeeId, @RequestParam YearMonth month,
                @RequestParam LocalDate toDate) {
                if (!YearMonth.from(toDate).equals(month)) throw new IllegalArgumentException("toDate must be in month");
                return attendanceService.monthlyBalance(employeeId, month, toDate);
        }
        }
