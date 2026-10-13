package com.example.demo.service;

import com.example.demo.dto.request.CreateEmployeeAccountRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

        private static final Logger log = LoggerFactory.getLogger(UserService.class);

        private final UserRepository userRepository;
        private final EmployeeRepository employeeRepository;
        private final PasswordEncoder passwordEncoder;

        public UserService(
                UserRepository userRepository,
                EmployeeRepository employeeRepository,
                PasswordEncoder passwordEncoder) {
                this.userRepository = userRepository;
                this.employeeRepository = employeeRepository;
                this.passwordEncoder = passwordEncoder;
        }

        public List<UserResponse> getAllUsers() {

                return userRepository.findAll()
                        .stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList());
        }

        @Transactional
        public UserResponse createEmployeeAccount(CreateEmployeeAccountRequest request) {
                String username = request.username().trim();
                if (username.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập username");
                }
                if (userRepository.existsByUsernameIgnoreCase(username)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Username đã tồn tại, vui lòng chọn username khác");
                }

                Employee employee = employeeRepository.findById(request.employeeId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên đã chọn"));

                if (userRepository.existsByEmployee_Id(employee.getId())) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT, "Nhân viên này đã có tài khoản");
                }

                User user = new User();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode(request.password()));
                user.setRole(Role.EMPLOYEE);
                user.setStatus(request.status() == null || request.status());
                user.setEmployee(employee);
                User savedUser = userRepository.save(user);
                log.info("event=employee_account_created userId={} employeeId={} role={}",
                                savedUser.getId(), employee.getId(), savedUser.getRole());
                return toResponse(savedUser);
        }

        public UserResponse getUserById(Long userId) {

                // Tìm user trong database
                User user = userRepository.findById(userId)
                        .orElseThrow(() ->
                                new RuntimeException("User not found with id: " + userId)
                        );
                return toResponse(user);
        }

        public UserResponse updateStatus(
                Long userId,
                boolean status,
                String currentAdminUsername
        ) {
                // Tìm user cần cập nhật
                User user = userRepository.findById(userId)
                        .orElseThrow(() ->
                                new RuntimeException("User not found with id: " + userId)
                        );

                //chặn admin tự tắt quyền
                if (user.getUsername().equalsIgnoreCase(currentAdminUsername)
                        && !status) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "Admin cannot deactivate own account"
                );
                }

                //true  -> Active
                // false -> Inactive
                user.setStatus(status);
                User savedUser = userRepository.save(user);
                log.info("event=user_status_changed userId={} status={} changedBy={}",
                                savedUser.getId(), savedUser.isStatus(), currentAdminUsername);
                return toResponse(savedUser);
        }

        private UserResponse toResponse(User user) {

                Employee employee = user.getEmployee();

                return new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getRole().name(),
                        user.isStatus(),
                        employee == null ? null : employee.getId(),
                        employee == null ? null : employee.getEmployeeName()
                );
        }
}
