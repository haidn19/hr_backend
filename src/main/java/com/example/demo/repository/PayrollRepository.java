package com.example.demo.repository;

import com.example.demo.entity.Employee;
import com.example.demo.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    Optional<Payroll> findByEmployeeAndMonth(Employee employee, YearMonth month);
    List<Payroll> findByEmployeeOrderByMonthDesc(Employee employee);
}