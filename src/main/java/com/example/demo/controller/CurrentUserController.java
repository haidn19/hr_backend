package com.example.demo.controller;

import com.example.demo.dto.response.CurrentUserResponse;
import com.example.demo.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController {
    private final CurrentUserService currentUserService;

    public CurrentUserController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/me")
    public CurrentUserResponse getMe(Authentication authentication) {
        return currentUserService.getCurrentUserResponse(authentication);
    }
}
