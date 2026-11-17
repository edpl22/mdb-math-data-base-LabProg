package com.mdb.mobile.model;

public class UserDto {
    public Long id;
    public String name;
    public String email;
    public String role;
    public String createdAt;

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
