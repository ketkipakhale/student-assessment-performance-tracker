package com.example.assessment.dto;

import java.util.List;

/** Admin-only: includes the answer key. */
public record AdminQuestionResponse(Long id, String text, int points, int order,
                                    List<AdminOptionResponse> options) {
}
