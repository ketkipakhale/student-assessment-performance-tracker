package com.example.assessment.dto;

import java.math.BigDecimal;
import java.util.List;

/** Chronological list of all attempts (oldest first) plus the average percentage across attempts. */
public record ProgressResponse(Long studentId, int attempts, BigDecimal averagePercentage,
                               List<ProgressEntryResponse> progress) {
}
