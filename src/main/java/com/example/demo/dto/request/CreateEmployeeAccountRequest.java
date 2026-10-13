package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEmployeeAccountRequest(
        @NotBlank(message = "Vui lòng nhập username")
        @Size(max = 100, message = "Username không được vượt quá 100 ký tự")
        String username,
        @NotBlank(message = "Vui lòng nhập mật khẩu")
        @Size(min = 8, max = 100, message = "Mật khẩu phải có từ 8 đến 100 ký tự")
        String password,
        @NotNull(message = "Vui lòng chọn nhân viên")
        Long employeeId,
        Boolean status) {
}
