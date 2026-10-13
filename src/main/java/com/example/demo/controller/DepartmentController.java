package com.example.demo.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.example.demo.dto.response.DepartmentResponse;
import com.example.demo.dto.response.DepartmentLeaderResponse;
import com.example.demo.dto.request.DepartmentRequest;
import com.example.demo.entity.Department;
import com.example.demo.service.DepartmentService;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public CompletableFuture<List<DepartmentResponse>> getDepartments() {
        return departmentService.getAllDepartments().thenApply(departments -> departments.stream()
                .map(this::toResponse)
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse createDepartment(@RequestBody DepartmentRequest request) {
        return toResponse(departmentService.createDepartment(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse updateDepartment(@PathVariable Long id, @RequestBody DepartmentRequest request) {
        return toResponse(departmentService.updateDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
    }

    private DepartmentResponse toResponse(Department department) {
        DepartmentLeaderResponse leader = department.getLeader() == null ? null
                : new DepartmentLeaderResponse(department.getLeader().getId(), department.getLeader().getEmployeeName());
        return new DepartmentResponse(department.getId(), department.getName(),
                departmentService.getEmployeeCount(department.getId()), leader);
    }
}