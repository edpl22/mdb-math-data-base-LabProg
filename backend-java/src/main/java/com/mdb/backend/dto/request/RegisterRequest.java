package com.mdb.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "O nome é obrigatório") @Size(max = 150) String name,
        @NotBlank(message = "O email é obrigatório") @Email(message = "Email inválido") @Size(max = 180) String email,
        @NotBlank(message = "A senha é obrigatória") @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres") String password
) {
}
