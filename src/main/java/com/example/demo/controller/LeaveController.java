package com.example.demo.controller;

import com.example.demo.dto.request.LeaveRequest;
import com.example.demo.entity.Leave;
import com.example.demo.service.LeaveService;
import com.example.demo.service.CurrentUserService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/leaves")
public class LeaveController {
    private final LeaveService leaveService;
    private final CurrentUserService currentUserService;
    public LeaveController(LeaveService leaveService, CurrentUserService currentUserService) {
        this.leaveService = leaveService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Leave> getAllLeaveRequests() {
        return leaveService.getAllLeaves();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<Leave> getMyLeaveRequests(Authentication authentication) {
        return leaveService.getLeavesByEmployeeId(currentUserService.getCurrentEmployee(authentication).getId());
    }

    // 1. EMPLOYEE TẠO ĐƠN NGHỈ PHÉP
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Leave> createLeaveRequest(
            @RequestBody LeaveRequest request) {

        Leave leave = leaveService.createLeave(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(leave);
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Leave> createMyLeaveRequest(
            @RequestBody LeaveRequest request, Authentication authentication) {
        request.setEmployeeId(currentUserService.getCurrentEmployee(authentication).getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.createLeave(request));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Leave> approveLeaveRequest(
            @PathVariable Long id) {
        Leave leave = leaveService.approveLeave(id);
        return ResponseEntity.ok(leave);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Leave> rejectLeaveRequest(
            @PathVariable Long id) {
        Leave leave = leaveService.rejectLeave(id);
        return ResponseEntity.ok(leave);
    }
}
