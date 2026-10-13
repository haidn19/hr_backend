package com.example.demo.controller;

import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.security.JwtService;

import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
        public class AuthController {

        private static final Logger log = LoggerFactory.getLogger(AuthController.class);

        private final AuthenticationManager authenticationManager;

        private final JwtService jwtService;

        public AuthController(
                AuthenticationManager authenticationManager,
                JwtService jwtService) {

                this.authenticationManager = authenticationManager;
                this.jwtService = jwtService;
        }

        @PostMapping("/login")
        public ResponseEntity<LoginResponse> login(
                @RequestBody LoginRequest request) {

                // Xác thực thông tin đăng nhập trước khi phát hành access token.
                Authentication authentication =
                        authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                        request.getUsername(),
                                        request.getPassword()
                                )
                        );

                // Token chứa username và role, được frontend gửi lại ở các request sau.
                String token =
                        jwtService.generateToken(authentication);

                log.info("event=login_succeeded username={}", authentication.getName());

                return ResponseEntity.ok(
                        new LoginResponse(
                                token,
                                "Bearer"
                        )
                );
        }
}
