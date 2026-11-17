package com.mdb.backend.dto.response;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
