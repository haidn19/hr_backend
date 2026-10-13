package com.example.demo.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import com.example.demo.entity.Employee;
import com.example.demo.service.EmployeeService;
import com.example.demo.service.CurrentUserService;
import com.example.demo.dto.request.EmployeeRequest;
import com.example.demo.dto.response.EmployeeResponse;
import com.example.demo.dto.response.DepartmentResponse;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class EmployeeController {
    private final EmployeeService employeeService;
    private final CurrentUserService currentUserService;

    public EmployeeController(EmployeeService employeeService, CurrentUserService currentUserService) {
        this.employeeService = employeeService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/employees/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public CompletableFuture<EmployeeResponse> getMyProfile(Authentication authentication) {
        return CompletableFuture.completedFuture(toAdminResponse(currentUserService.getCurrentEmployee(authentication)));
    }

    @GetMapping("/employees")
    public CompletableFuture<List<EmployeeResponse>> getEmployees(Authentication authentication) {
        // ADMIN được xem đầy đủ; USER chỉ nhận dữ liệu rút gọn.
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return employeeService.getAllEmployees().thenApply(employees -> employees.stream()
                .map(employee -> toResponse(employee, admin))
                .toList());
    }

    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<EmployeeResponse> createEmployee(@RequestBody EmployeeRequest request) {
        // Chỉ ADMIN được phép thay đổi dữ liệu nhân viên.
        return employeeService.createEmployee(request).thenApply(this::toAdminResponse);
    }

    @GetMapping("/employees/{id}")
    public CompletableFuture<EmployeeResponse> getEmployee(@PathVariable Long id, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return CompletableFuture.completedFuture(toResponse(employeeService.getEmployeeById(id), admin));
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<EmployeeResponse> updateEmployee(@PathVariable Long id, @RequestBody EmployeeRequest request) {
        return employeeService.updateEmployee(id, request).thenApply(this::toAdminResponse);
    }

    @PatchMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<EmployeeResponse> patchEmployee(@PathVariable Long id, @RequestBody EmployeeRequest request) {
        return employeeService.patchEmployee(id, request).thenApply(this::toAdminResponse);
    }

    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<Void> deleteEmployee(@PathVariable Long id) {
        return employeeService.deleteEmployee(id);
    }

    private EmployeeResponse toResponse(Employee employee, boolean admin) {
        return admin ? toAdminResponse(employee) : EmployeeResponse.staffView(employee.getId(), employee.getEmployeeName());
    }

    private EmployeeResponse toAdminResponse(Employee employee) {
        DepartmentResponse department = employee.getDepartment() == null ? null
            : new DepartmentResponse(employee.getDepartment().getId(), employee.getDepartment().getName(), 0, null);
        return new EmployeeResponse(employee.getId(), employee.getEmployeeName(), employee.getDateOfBirth(),
                employee.getPhoneNumber(), employee.getAddress(), employee.getEmail(), employee.getTaxCode(),
                department, employee.getBaseSalary(), BigDecimal.valueOf(employee.getLeaveDays()));
    }
}
