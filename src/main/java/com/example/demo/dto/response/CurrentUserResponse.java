package com.example.demo.dto.response;

public record CurrentUserResponse(
        Long userId,
        String username,
        String role,
        Long employeeId,
        String employeeName) {
}
