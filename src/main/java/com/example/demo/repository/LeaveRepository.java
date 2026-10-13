package com.example.demo.repository;

import com.example.demo.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRepository extends JpaRepository<Leave, Long> {
        List<Leave> findAllByOrderByIdDesc();

        List<Leave> findByEmployeeId(Long employeeId);

        List<Leave> findByStatus(String status);

        List<Leave> findByEmployeeIdAndStatus(Long employeeId, String status);

        long countByEmployeeIdAndStatus(Long employeeId, String status);

        List<Leave> findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                Long employeeId, String status, LocalDate toDate, LocalDate fromDate);

        boolean existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                Long employeeId, List<String> statuses, LocalDate toDate, LocalDate fromDate);
        }
