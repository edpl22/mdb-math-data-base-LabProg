package com.mdb.backend.security;

import com.mdb.backend.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class AuthenticatedUser extends User {

    private final Long id;
    private final Role role;

    public AuthenticatedUser(Long id, String email, Role role) {
        super(email, "", authorities(role));
        this.id = id;
        this.role = role;
    }

    private static List<GrantedAuthority> authorities(Role role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }
}
