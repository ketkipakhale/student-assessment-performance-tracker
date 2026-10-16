package com.example.assessment.dto;

/**
 * Feedback on one question of a saved submission. selectedOptionId is null when the question was
 * unanswered. The correct option itself is never revealed.
 */
public record AnswerResultResponse(Long questionId, String questionText, Long selectedOptionId,
                                   boolean answered, boolean correct, int pointsAwarded, int pointsPossible) {
}
