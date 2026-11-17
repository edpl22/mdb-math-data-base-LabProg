package com.mdb.backend.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void shouldRejectProblemWithoutSolution() {
        ProblemRequest request = new ProblemRequest(
                "Valid title", "Valid statement", "Algebra", "Valid hint", "", 3, List.of(), null);

        Set<ConstraintViolation<ProblemRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("solution"));
    }

    @Test
    void shouldRejectProblemWithoutHint() {
        ProblemRequest request = new ProblemRequest(
                "Valid title", "Valid statement", "Algebra", "", "Valid solution", 3, List.of(), null);

        Set<ConstraintViolation<ProblemRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("hint"));
    }

    @Test
    void shouldRejectDifficultyAboveFive() {
        ProblemRequest request = new ProblemRequest(
                "Valid title", "Valid statement", "Algebra", "Hint", "Solution", 8, List.of(), null);

        Set<ConstraintViolation<ProblemRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("difficulty"));
    }

    @Test
    void shouldRejectDifficultyBelowOne() {
        ProblemRequest request = new ProblemRequest(
                "Valid title", "Valid statement", "Algebra", "Hint", "Solution", 0, List.of(), null);

        Set<ConstraintViolation<ProblemRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("difficulty"));
    }

    @Test
    void shouldAcceptFullyValidProblem() {
        ProblemRequest request = new ProblemRequest(
                "Valid title", "Valid statement", "Algebra", "Valid hint", "Valid solution",
                3, List.of("equations"), "Optional source");

        Set<ConstraintViolation<ProblemRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
