package com.example.demo.service;

import com.example.demo.dto.response.CurrentUserResponse;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản đăng nhập"));
    }

    public Employee getCurrentEmployee(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user.getRole() != Role.EMPLOYEE || user.getEmployee() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không có hồ sơ nhân viên");
        }
        return user.getEmployee();
    }

    public CurrentUserResponse getCurrentUserResponse(Authentication authentication) {
        User user = getCurrentUser(authentication);
        Employee employee = user.getEmployee();
        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getRole().name(),
                employee == null ? null : employee.getId(),
                employee == null ? null : employee.getEmployeeName());
    }
}
