package com.example.assessment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SubmissionResultResponse(Long submissionId, Long studentId, Long assessmentId,
                                       String assessmentTitle, int score, int totalPoints,
                                       BigDecimal percentage, Instant submittedAt) {
}
