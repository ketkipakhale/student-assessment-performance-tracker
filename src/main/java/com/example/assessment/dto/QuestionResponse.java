package com.example.assessment.dto;

import java.util.List;

public record QuestionResponse(Long id, String text, int points, int order, List<OptionResponse> options) {
}
