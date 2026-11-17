package com.mdb.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("token").asText();
    }

    @Test
    void catalog_shouldBePubliclyReadable() throws Exception {
        mockMvc.perform(get("/api/problems").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void search_shouldFilterByAreaAndText() throws Exception {
        mockMvc.perform(get("/api/problems")
                        .param("area", "Algebra")
                        .param("text", "quadratic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Simple quadratic equation"));
    }

    @Test
    void createProblem_withoutAuthentication_shouldReturn401() throws Exception {
        String body = """
                {"title":"X","statement":"Y","area":"Algebra","hint":"H","solution":"S","difficulty":2,"tags":[]}
                """;
        mockMvc.perform(post("/api/problems").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProblem_asRegularUser_shouldReturn403() throws Exception {
        String token = loginAndGetToken("demo@mdb.dev", "demo123");
        String body = """
                {"title":"X","statement":"Y","area":"Algebra","hint":"H","solution":"S","difficulty":2,"tags":[]}
                """;
        mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProblem_asAdmin_shouldSucceedAndBeRetrievable() throws Exception {
        String token = loginAndGetToken("admin@mdb.dev", "admin123");
        String body = """
                {"title":"Test problem","statement":"Test statement","area":"Algebra",
                 "hint":"Test hint","solution":"Test solution","difficulty":3,"tags":["test"]}
                """;
        mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test problem"));
    }

    @Test
    void createProblem_withInvalidDifficulty_shouldReturn400() throws Exception {
        String token = loginAndGetToken("admin@mdb.dev", "admin123");
        String body = """
                {"title":"X","statement":"Y","area":"Algebra","hint":"H","solution":"S","difficulty":8,"tags":[]}
                """;
        mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProblem_withoutSolution_shouldReturn400() throws Exception {
        String token = loginAndGetToken("admin@mdb.dev", "admin123");
        String body = """
                {"title":"X","statement":"Y","area":"Algebra","hint":"H","solution":"","difficulty":2,"tags":[]}
                """;
        mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProblem_asRegularUser_shouldReturn403() throws Exception {
        String token = loginAndGetToken("demo@mdb.dev", "demo123");
        mockMvc.perform(delete("/api/problems/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void rateDifficultyTwice_shouldReturn409OnSecondAttempt() throws Exception {
        String token = loginAndGetToken("admin@mdb.dev", "admin123");
        // problem 4 (2x2 linear system) not yet rated by admin
        String body = """
                {"difficulty":3}
                """;
        mockMvc.perform(post("/api/problems/4/difficulty")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/problems/4/difficulty")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void favoriteAndUnfavorite_shouldToggleStatusList() throws Exception {
        String token = loginAndGetToken("admin@mdb.dev", "admin123");
        mockMvc.perform(put("/api/problems/6/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"status\":\"FAVORITE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statuses[0]").value("FAVORITE"));

        mockMvc.perform(delete("/api/problems/6/status")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "FAVORITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statuses").isEmpty());
    }

    @Test
    void regularUser_canMarkStatus_withoutAdminRole() throws Exception {
        String token = loginAndGetToken("demo@mdb.dev", "demo123");
        mockMvc.perform(put("/api/problems/7/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"status\":\"WANT_TO_DO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statuses[0]").value("WANT_TO_DO"));
    }

    @Test
    void regularUser_canRateDifficulty_withoutAdminRole() throws Exception {
        // problem 5 (Triangle area by coordinates) not yet rated by the demo user
        String token = loginAndGetToken("demo@mdb.dev", "demo123");
        mockMvc.perform(post("/api/problems/5/difficulty")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"difficulty\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userDifficulty").value(3));
    }
}
