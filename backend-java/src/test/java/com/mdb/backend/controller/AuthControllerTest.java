package com.mdb.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void register_thenLogin_shouldSucceed() throws Exception {
        String registerBody = """
                {"name":"Novo Usuário","email":"novo@mdb.dev","password":"senha123"}
                """;
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("USER"));

        String loginBody = """
                {"email":"novo@mdb.dev","password":"senha123"}
                """;
        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void register_withDuplicateEmail_shouldReturn409() throws Exception {
        String body = """
                {"name":"Admin Clone","email":"admin@mdb.dev","password":"senha123"}
                """;
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void register_withShortPassword_shouldReturn400() throws Exception {
        String body = """
                {"name":"Teste","email":"teste@mdb.dev","password":"123"}
                """;
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withWrongPassword_shouldReturn401() throws Exception {
        String body = """
                {"email":"admin@mdb.dev","password":"senha-errada"}
                """;
        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }
}
