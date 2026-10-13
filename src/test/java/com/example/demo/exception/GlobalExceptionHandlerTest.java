package com.example.demo.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    @Test void responseContainsMessageAndDoesNotContainPath() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn nghỉ phép đã được xử lý"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Đơn nghỉ phép đã được xử lý", response.getBody().get("message"));
        assertFalse(response.getBody().containsKey("path"));
    }

    @Test void businessErrorUsesItsErrorCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatusException(
                new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_LEAVE_BALANCE",
                        "Số ngày phép còn lại không đủ"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("INSUFFICIENT_LEAVE_BALANCE", response.getBody().get("error"));
        assertEquals("Số ngày phép còn lại không đủ", response.getBody().get("message"));
    }
}
