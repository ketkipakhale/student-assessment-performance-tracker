package com.example.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "One answer choice of a question")
public record OptionRequest(
        @Schema(example = "A stack") @NotBlank @Size(max = 1000) String text,
        @Schema(description = "Exactly one option per question must be marked correct", example = "true")
        @NotNull Boolean correct) {
}
