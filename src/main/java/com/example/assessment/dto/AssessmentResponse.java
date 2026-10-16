package com.example.assessment.dto;

import java.util.List;

/** Student-facing view of a published assessment (no answer key). */
public record AssessmentResponse(Long id, String title, String description, int totalPoints,
                                 List<QuestionResponse> questions) {
}
