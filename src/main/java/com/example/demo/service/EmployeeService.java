package com.example.demo.service;

import com.example.demo.entity.Employee;
import com.example.demo.repository.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.dto.request.EmployeeRequest;
import com.example.demo.entity.Department;
import com.example.demo.repository.DepartmentRepository;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private static final Logger log =
        LoggerFactory.getLogger(EmployeeService.class);

    public EmployeeService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    @Async
    public CompletableFuture<List<Employee>> getAllEmployees() {
        return CompletableFuture.completedFuture(employeeRepository.findAll());
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @Async
    public CompletableFuture<Employee> createEmployee(EmployeeRequest request) {
        // Chuyển request thành entity và kiểm tra department trước khi lưu.
        Employee employee = employeeRepository.save(toEntity(request));
        log.info("event=employee_created employeeId={} departmentId={}",
                employee.getId(), employee.getDepartment().getId());
        return CompletableFuture.completedFuture(employee);
    }

    @Async
    public CompletableFuture<Employee> updateEmployee(Long id, EmployeeRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "employeeName is required");
        }

        Employee existingEmployee = getEmployeeById(id);
        copyFields(existingEmployee, request);
        Employee employee = employeeRepository.save(existingEmployee);
        log.info("event=employee_updated employeeId={} departmentId={}",
                employee.getId(), employee.getDepartment().getId());
        return CompletableFuture.completedFuture(employee);
    }

    @Async
    public CompletableFuture<Employee> patchEmployee(Long id, EmployeeRequest request) {
        // PATCH chỉ ghi đè các trường khác null, giữ nguyên phần còn lại.
        Employee existingEmployee = getEmployeeById(id);
        if (request != null) {
            if (request.name() != null) existingEmployee.setEmployeeName(request.name());
            if (request.dateOfBirth() != null) existingEmployee.setDateOfBirth(request.dateOfBirth());
            if (request.phoneNumber() != null) existingEmployee.setPhoneNumber(request.phoneNumber());
            if (request.address() != null) existingEmployee.setAddress(request.address());
            if (request.email() != null) existingEmployee.setEmail(request.email());
            if (request.taxCode() != null) existingEmployee.setTaxCode(request.taxCode());
            if (request.departmentId() != null) existingEmployee.setDepartment(getDepartment(request.departmentId()));
            if (request.baseSalary() != null) existingEmployee.setBaseSalary(request.baseSalary());
            if (request.LeaveDays() != null) existingEmployee.setLeaveDays(request.LeaveDays());
        }
        Employee employee = employeeRepository.save(existingEmployee);
        log.info("event=employee_patched employeeId={} departmentId={}",
                employee.getId(), employee.getDepartment().getId());
        return CompletableFuture.completedFuture(employee);
    }

    @Async
    public CompletableFuture<Void> deleteEmployee(Long id) {
        employeeRepository.deleteById(id);
        log.info("event=employee_deleted employeeId={}", id);
        return CompletableFuture.completedFuture(null);
    }

    private Employee toEntity(EmployeeRequest request) {
        Employee employee = new Employee();
        copyFields(employee, request);
        return employee;
    }

    private void copyFields(Employee employee, EmployeeRequest request) {
        // Employee bắt buộc phải thuộc một department hợp lệ.
        if (request.departmentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "departmentId is required");
        }
        employee.setEmployeeName(request.name());
        employee.setDateOfBirth(request.dateOfBirth());
        employee.setPhoneNumber(request.phoneNumber());
        employee.setAddress(request.address());
        employee.setEmail(request.email());
        employee.setTaxCode(request.taxCode());
        employee.setBaseSalary(request.baseSalary());
        employee.setLeaveDays(request.LeaveDays() == null
            ? 0 : request.LeaveDays());
        employee.setDepartment(getDepartment(request.departmentId()));
    }

    private Department getDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"));
    }
}
