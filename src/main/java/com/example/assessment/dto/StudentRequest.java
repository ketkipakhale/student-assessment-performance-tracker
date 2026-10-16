package com.example.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentRequest(
        @Schema(example = "Asha Patil") @NotBlank @Size(max = 150) String fullName,
        @Schema(example = "asha.patil@example.com") @NotBlank @Email @Size(max = 255) String email,
        @Schema(example = "a-strong-password") @Size(min = 8, max = 100) String password) {
}
