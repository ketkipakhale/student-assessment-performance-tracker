package com.example.assessment.dto;

import com.example.assessment.entity.AssessmentStatus;

import java.time.Instant;
import java.util.List;

/** Admin-only view of an assessment, including which options are correct. */
public record AdminAssessmentResponse(Long id, String title, String description, AssessmentStatus status,
                                      int totalPoints, Instant createdAt, Instant updatedAt,
                                      List<AdminQuestionResponse> questions) {
}
