package com.example.assessment.dto;

import com.example.assessment.entity.AssessmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Full definition of an assessment (used for create and for full replace)")
public record AssessmentRequest(
        @Schema(example = "Java Basics Quiz") @NotBlank @Size(max = 200) String title,
        @Schema(example = "Covers core Java concepts") @Size(max = 2000) String description,
        @Schema(description = "DRAFT (default) or PUBLISHED. Students can only submit PUBLISHED assessments.",
                example = "PUBLISHED")
        AssessmentStatus status,
        @NotEmpty List<@Valid @jakarta.validation.constraints.NotNull QuestionRequest> questions) {
}
