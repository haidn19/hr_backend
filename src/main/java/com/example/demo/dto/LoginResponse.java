package com.example.demo.dto;

public class LoginResponse {
        private final String token;
        private final String tokenType;

        public LoginResponse(String token, String tokenType) {
        this.token = token;
        this.tokenType = tokenType;
        }

        public String getToken() {
        return token;
        }

        public String getTokenType() {
        return tokenType;
        }

}
