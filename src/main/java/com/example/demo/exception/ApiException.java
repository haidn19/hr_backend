package com.example.demo.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class ApiException extends ResponseStatusException {
    private final String errorCode;

    public ApiException(HttpStatusCode status, String errorCode, String message) {
        super(status, message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
