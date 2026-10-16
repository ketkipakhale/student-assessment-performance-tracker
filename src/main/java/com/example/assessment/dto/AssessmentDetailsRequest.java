package com.example.assessment.dto;

import com.example.assessment.entity.AssessmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Partial update of title / description / status. Omitted (null) fields are left unchanged.")
public record AssessmentDetailsRequest(
        @Size(max = 200) String title,
        @Size(max = 2000) String description,
        AssessmentStatus status) {
}
