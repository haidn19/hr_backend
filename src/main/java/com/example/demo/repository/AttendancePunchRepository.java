package com.example.demo.repository;

import com.example.demo.entity.AttendancePunch;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendancePunchRepository extends JpaRepository<AttendancePunch, Long> {
    List<AttendancePunch> findByEmployeeIdAndPunchedAtGreaterThanEqualAndPunchedAtLessThan(
            Long employeeId, LocalDateTime from, LocalDateTime to);
}
