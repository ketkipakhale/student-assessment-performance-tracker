package com.example.assessment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SubmissionDetailResponse(Long submissionId, Long studentId, Long assessmentId,
                                       String assessmentTitle, int score, int totalPoints,
                                       BigDecimal percentage, Instant submittedAt,
                                       int answeredCount, int unansweredCount,
                                       List<AnswerResultResponse> answers) {
}
