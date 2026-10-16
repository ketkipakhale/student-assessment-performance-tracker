package com.example.assessment.dto;

import java.time.Instant;

public record StudentResponse(Long id, String fullName, String email, Instant createdAt) {
}
