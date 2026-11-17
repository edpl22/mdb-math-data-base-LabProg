package com.mdb.backend.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

public record ProblemRequest(
        @NotBlank(message = "O título é obrigatório") @Size(max = 200) String title,
        @NotBlank(message = "O enunciado é obrigatório") String statement,
        @NotBlank(message = "A área é obrigatória") String area,
        @NotBlank(message = "A dica é obrigatória") String hint,
        @NotBlank(message = "A solução é obrigatória") String solution,
        @NotNull(message = "A dificuldade inicial é obrigatória")
        @Min(value = 1, message = "A dificuldade deve estar entre 1 e 5")
        @Max(value = 5, message = "A dificuldade deve estar entre 1 e 5")
        Integer difficulty,
        List<String> tags,
        @Size(max = 255) String source
) {
}
