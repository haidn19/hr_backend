package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.request.CreateEmployeeAccountRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.Employee;
import com.example.demo.entity.User;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class UserServiceTest {

    @Test
    void createsAnEmployeeAccountWithEncodedPasswordAndLinkedEmployee() {
        UserRepository users = mock(UserRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Employee employee = new Employee("Nguyễn Văn An");
        employee.setId(10L);
        CreateEmployeeAccountRequest request = new CreateEmployeeAccountRequest(
                "nvan", "temporary-password", 10L, true);

        when(employees.findById(10L)).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("temporary-password")).thenReturn("encoded-password");
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(5L);
            return user;
        });

        UserResponse result = new UserService(users, employees, passwordEncoder)
                .createEmployeeAccount(request);

        assertEquals(5L, result.getId());
        assertEquals("nvan", result.getUsername());
        assertEquals("EMPLOYEE", result.getRole());
        assertEquals(10L, result.getEmployeeId());
        assertEquals("Nguyễn Văn An", result.getEmployeeName());
        verify(users).save(any(User.class));
    }

    @Test
    void rejectsDuplicateUsername() {
        UserRepository users = mock(UserRepository.class);
        when(users.existsByUsernameIgnoreCase("nvan")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new UserService(users, mock(EmployeeRepository.class), mock(PasswordEncoder.class))
                        .createEmployeeAccount(new CreateEmployeeAccountRequest(
                                "nvan", "temporary-password", 10L, true)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Username đã tồn tại, vui lòng chọn username khác", exception.getReason());
    }

    @Test
    void rejectsAnEmployeeThatAlreadyHasAnAccount() {
        UserRepository users = mock(UserRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        Employee employee = new Employee("Nguyễn Văn An");
        employee.setId(10L);
        when(employees.findById(10L)).thenReturn(Optional.of(employee));
        when(users.existsByEmployee_Id(10L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new UserService(users, employees, mock(PasswordEncoder.class))
                        .createEmployeeAccount(new CreateEmployeeAccountRequest(
                                "nvan", "temporary-password", 10L, true)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Nhân viên này đã có tài khoản", exception.getReason());
    }
}
