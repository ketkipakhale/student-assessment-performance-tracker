package com.example.assessment.dto;

import com.example.assessment.entity.AssessmentStatus;

public record AssessmentSummaryResponse(Long id, String title, String description, AssessmentStatus status,
                                        int questionCount, int totalPoints) {
}
