package com.example.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "A student's answers. Questions that are left out count as unanswered (0 points).")
public record SubmissionRequest(
        Long studentId,
        @NotEmpty List<@Valid @NotNull AnswerRequest> answers) {
}
