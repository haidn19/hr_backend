package com.example.demo.dto.response;

public class UserResponse {

    private Long id;
    private String username;
    private String role;
    private boolean status;
    private Long employeeId;
    private String employeeName;

    public UserResponse() {
    }

    public UserResponse(
            Long id,
            String username,
            String role,
            boolean status,
            Long employeeId,
            String employeeName
    ) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.status = status;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public boolean isStatus() {
        return status;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

}
