package com.mdb.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AreaRequest(
        @NotBlank(message = "O nome da área é obrigatório") @Size(max = 100) String name
) {
}
