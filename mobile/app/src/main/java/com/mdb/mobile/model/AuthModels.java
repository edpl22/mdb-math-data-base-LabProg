package com.mdb.mobile.model;

public class AuthModels {

    public static class RegisterRequest {
        public String name;
        public String email;
        public String password;

        public RegisterRequest(String name, String email, String password) {
            this.name = name;
            this.email = email;
            this.password = password;
        }
    }

    public static class LoginRequest {
        public String email;
        public String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class AuthResponse {
        public String token;
        public UserDto user;
    }
}
