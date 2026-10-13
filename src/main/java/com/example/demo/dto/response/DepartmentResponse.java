package com.example.demo.dto.response;

public record DepartmentResponse(Long id, String name, long employeeCount, DepartmentLeaderResponse leader) {
}