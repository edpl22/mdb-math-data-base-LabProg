package com.mdb.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DifficultyRatingRequest(
        @NotNull(message = "A dificuldade é obrigatória")
        @Min(value = 1, message = "A dificuldade deve estar entre 1 e 5")
        @Max(value = 5, message = "A dificuldade deve estar entre 1 e 5")
        Integer difficulty
) {
}
