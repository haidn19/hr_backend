package com.example.demo.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.entity.Department;
import com.example.demo.entity.Employee;
import com.example.demo.dto.request.DepartmentRequest;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.EmployeeRepository;

@Service
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }
// Lấy danh sách tất cả các phòng ban, sắp xếp theo tên.
    @Async
    public CompletableFuture<List<Department>> getAllDepartments() {
        return CompletableFuture.completedFuture(departmentRepository.findAllByOrderByNameAsc());
    }
// tìm phòng ban theo id (400)
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"));
    }
// số nhân viên trong phòng ban
    public long getEmployeeCount(Long departmentId) {
        return employeeRepository.countByDepartmentId(departmentId);
    }
// tạo phòng ban mới
    public Department createDepartment(DepartmentRequest request) {
        String name = normalizeName(request.name());
        ensureNameAvailable(name, null);
        Department department = new Department(name);
        department.setLeader(resolveLeader(request.leaderId()));
        Department savedDepartment = departmentRepository.save(department);
        log.info("event=department_created departmentId={} leaderId={}",
                savedDepartment.getId(),
                savedDepartment.getLeader() == null ? null : savedDepartment.getLeader().getId());
        return savedDepartment;
    }
// update
    public Department updateDepartment(Long id, DepartmentRequest request) {
        Department department = getDepartmentById(id);
        String name = normalizeName(request.name());
        ensureNameAvailable(name, id);
        department.setName(name);
        department.setLeader(resolveLeader(request.leaderId()));
        Department savedDepartment = departmentRepository.save(department);
        log.info("event=department_updated departmentId={} leaderId={}",
                savedDepartment.getId(),
                savedDepartment.getLeader() == null ? null : savedDepartment.getLeader().getId());
        return savedDepartment;
    }
// xóa
    public void deleteDepartment(Long id) {
        Department department = getDepartmentById(id);
        if (employeeRepository.countByDepartmentId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete a department that still has employees");
        }
        departmentRepository.delete(department);
        log.info("event=department_deleted departmentId={}", id);
    }
// loại bỏ khoảng trắng
    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department name is required");
        }
        return name.trim();
    }
// không trùng tên
    private void ensureNameAvailable(String name, Long currentId) {
        departmentRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Department name already exists");
            }
        });
    }
// tìm leader theo id
    private Employee resolveLeader(Long leaderId) {
        if (leaderId == null) {
            return null;
        }
        return employeeRepository.findById(leaderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leader not found"));
    }
}
