package com.example.assessment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProgressEntryResponse(Long submissionId, Long assessmentId, String assessmentTitle,
                                    int score, int totalPoints, BigDecimal percentage, Instant submittedAt) {
}
