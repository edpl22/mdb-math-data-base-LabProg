package com.mdb.backend.dto.request;

import com.mdb.backend.entity.StatusType;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(
        @NotNull(message = "O status é obrigatório") StatusType status
) {
}
