package com.example.demo.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {
        Optional<Attendance> findByEmployeeIdAndWorkDate(
                Long employeeId,
                LocalDate workDate
        );
        List<Attendance> findByEmployeeIdAndWorkDateBetween(
                Long employeeId,
                LocalDate fromDate,
                LocalDate toDate
        );
}