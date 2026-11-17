package com.mdb.backend.service;

import com.mdb.backend.dto.request.LoginRequest;
import com.mdb.backend.dto.request.RegisterRequest;
import com.mdb.backend.dto.response.AuthResponse;
import com.mdb.backend.entity.Role;
import com.mdb.backend.entity.User;
import com.mdb.backend.exception.DuplicateResourceException;
import com.mdb.backend.exception.InvalidCredentialsException;
import com.mdb.backend.repository.UserRepository;
import com.mdb.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Fulano", "fulano@mdb.dev", "senha123");
        when(userRepository.existsByEmail("fulano@mdb.dev")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldHashPasswordAndReturnToken() {
        RegisterRequest request = new RegisterRequest("Fulano", "fulano@mdb.dev", "senha123");
        when(userRepository.existsByEmail("fulano@mdb.dev")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hashed");
        User saved = User.builder()
                .id(1L).name("Fulano").email("fulano@mdb.dev")
                .passwordHash("hashed").role(Role.USER).createdAt(LocalDateTime.now()).build();
        when(userRepository.save(any())).thenReturn(saved);
        when(jwtService.generateToken(1L, "fulano@mdb.dev", "USER")).thenReturn("token-123");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("token-123");
        assertThat(response.user().email()).isEqualTo("fulano@mdb.dev");
        verify(passwordEncoder).encode("senha123");
    }

    @Test
    void login_shouldRejectWrongPassword() {
        User user = User.builder()
                .id(1L).name("Fulano").email("fulano@mdb.dev")
                .passwordHash("hashed").role(Role.USER).createdAt(LocalDateTime.now()).build();
        when(userRepository.findByEmail("fulano@mdb.dev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        LoginRequest request = new LoginRequest("fulano@mdb.dev", "wrong");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_shouldRejectUnknownEmail() {
        when(userRepository.findByEmail("ghost@mdb.dev")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("ghost@mdb.dev", "whatever");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
