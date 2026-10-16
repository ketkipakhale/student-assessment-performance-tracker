package com.example.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "A multiple-choice question")
public record QuestionRequest(
        @Schema(example = "Which data structure uses LIFO order?") @NotBlank @Size(max = 2000) String text,
        @Schema(description = "Points awarded for a correct answer (default 1)", example = "2")
        @Min(1) @Max(100) Integer points,
        @Schema(description = "At least 2 options, exactly one marked correct")
        @NotNull @Size(min = 2, max = 10, message = "must contain between 2 and 10 options")
        List<@Valid @NotNull OptionRequest> options) {
}
