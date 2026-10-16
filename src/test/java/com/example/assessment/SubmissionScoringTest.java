package com.example.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionScoringTest extends IntegrationTestSupport {

    @Test
    void allCorrectAnswersScoreFullMarks() throws Exception {
        JsonNode a = createAssessment("Scoring", "PUBLISHED");
        long studentId = createStudent("full@example.com");

        JsonNode result = submit(a.get("id").asLong(), submissionBody(studentId,
                answer(questionId(a, 0), optionId(a, 0, "B")),
                answer(questionId(a, 1), optionId(a, 1, "X"))), 201);

        assertThat(result.get("submissionId").asLong()).isPositive();
        assertThat(result.get("score").asInt()).isEqualTo(5);
        assertThat(result.get("totalPoints").asInt()).isEqualTo(5);
        assertThat(result.get("percentage").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(result.get("submittedAt").asText()).isNotBlank();
        assertThat(result.get("assessmentTitle").asText()).isEqualTo("Scoring");
    }

    @Test
    void mixedAnswersUseQuestionPoints() throws Exception {
        JsonNode a = createAssessment("Mixed", "PUBLISHED");
        long studentId = createStudent("mixed@example.com");

        // Q1 wrong (A), Q2 right (X) -> 3 of 5 points = 60%
        JsonNode result = submit(a.get("id").asLong(), submissionBody(studentId,
                answer(questionId(a, 0), optionId(a, 0, "A")),
                answer(questionId(a, 1), optionId(a, 1, "X"))), 201);

        assertThat(result.get("score").asInt()).isEqualTo(3);
        assertThat(result.get("percentage").decimalValue()).isEqualByComparingTo("60.00");
    }

    @Test
    void unansweredQuestionsScoreZeroButStillCountInTotal() throws Exception {
        JsonNode a = createAssessment("Partial", "PUBLISHED");
        long studentId = createStudent("partial@example.com");

        // Only Q1 answered correctly (2 pts); Q2 (3 pts) left out.
        JsonNode result = submit(a.get("id").asLong(), submissionBody(studentId,
                answer(questionId(a, 0), optionId(a, 0, "B"))), 201);

        assertThat(result.get("score").asInt()).isEqualTo(2);
        assertThat(result.get("totalPoints").asInt()).isEqualTo(5);
        assertThat(result.get("percentage").decimalValue()).isEqualByComparingTo("40.00");

        JsonNode detail = get("/api/submissions/" + result.get("submissionId").asLong(), 200);
        assertThat(detail.get("answeredCount").asInt()).isEqualTo(1);
        assertThat(detail.get("unansweredCount").asInt()).isEqualTo(1);
        JsonNode unanswered = detail.get("answers").get(1);
        assertThat(unanswered.get("answered").asBoolean()).isFalse();
        assertThat(unanswered.get("selectedOptionId").isNull()).isTrue();
        assertThat(unanswered.get("pointsAwarded").asInt()).isZero();
    }

    @Test
    void clientCannotInjectItsOwnScore() throws Exception {
        JsonNode a = createAssessment("Tamper", "PUBLISHED");
        long studentId = createStudent("tamper@example.com");

        var body = submissionBody(studentId, answer(questionId(a, 0), optionId(a, 0, "A")));
        body.put("score", 5);
        body.put("percentage", 100);

        JsonNode result = submit(a.get("id").asLong(), body, 201);
        assertThat(result.get("score").asInt()).isZero();
    }

    @Test
    void unknownStudentIsRejectedWith404() throws Exception {
        JsonNode a = createAssessment("Unknown student", "PUBLISHED");
        submit(a.get("id").asLong(), submissionBody(424242,
                answer(questionId(a, 0), optionId(a, 0, "B"))), 404);
    }

    @Test
    void unknownAssessmentIsRejectedWith404() throws Exception {
        long studentId = createStudent("noassessment@example.com");
        submit(424242, submissionBody(studentId, answer(1, 1)), 404);
    }

    @Test
    void draftAssessmentCannotBeSubmittedAndReturns409() throws Exception {
        JsonNode a = createAssessment("Draft", "DRAFT");
        long studentId = createStudent("draft@example.com");
        submit(a.get("id").asLong(), submissionBody(studentId,
                answer(questionId(a, 0), optionId(a, 0, "B"))), 409);
    }

    @Test
    void questionFromAnotherAssessmentIsRejected() throws Exception {
        JsonNode a = createAssessment("First", "PUBLISHED");
        JsonNode other = createAssessment("Second", "PUBLISHED");
        long studentId = createStudent("wrongq@example.com");

        JsonNode error = submit(a.get("id").asLong(), submissionBody(studentId,
                answer(questionId(other, 0), optionId(other, 0, "B"))), 400);
        assertThat(error.get("message").asText()).contains("does not belong to assessment");
    }

    @Test
    void nonexistentQuestionIdIsRejected() throws Exception {
        JsonNode a = createAssessment("Bad question id", "PUBLISHED");
        long studentId = createStudent("noq@example.com");
        submit(a.get("id").asLong(), submissionBody(studentId, answer(99999999, 1)), 400);
    }

    @Test
    void optionBelongingToADifferentQuestionIsRejected() throws Exception {
        JsonNode a = createAssessment("Wrong option", "PUBLISHED");
        long studentId = createStudent("wrongopt@example.com");

        // Option "X" belongs to question 2, but is submitted for question 1.
        JsonNode error = submit(a.get("id").asLong(), submissionBody(studentId,
                answer(questionId(a, 0), optionId(a, 1, "X"))), 400);
        assertThat(error.get("message").asText()).contains("does not belong to question");
    }

    @Test
    void nonexistentOptionIdIsRejected() throws Exception {
        JsonNode a = createAssessment("Bad option id", "PUBLISHED");
        long studentId = createStudent("noopt@example.com");
        submit(a.get("id").asLong(), submissionBody(studentId, answer(questionId(a, 0), 99999999)), 400);
    }

    @Test
    void duplicateAnswersForTheSameQuestionAreRejected() throws Exception {
        JsonNode a = createAssessment("Duplicates", "PUBLISHED");
        long studentId = createStudent("dupans@example.com");

        JsonNode error = submit(a.get("id").asLong(), submissionBody(studentId,
                answer(questionId(a, 0), optionId(a, 0, "A")),
                answer(questionId(a, 0), optionId(a, 0, "B"))), 400);
        assertThat(error.get("message").asText()).contains("Duplicate answer");
    }

    @Test
    void emptyAnswerListIsRejectedByValidation() throws Exception {
        JsonNode a = createAssessment("Empty", "PUBLISHED");
        long studentId = createStudent("empty@example.com");
        submit(a.get("id").asLong(), submissionBody(studentId), 400);
    }

    @Test
    void retakesAreSavedAsSeparateSubmissions() throws Exception {
        JsonNode a = createAssessment("Retake", "PUBLISHED");
        long id = a.get("id").asLong();
        long studentId = createStudent("retake@example.com");

        JsonNode first = submit(id, submissionBody(studentId,
                answer(questionId(a, 0), optionId(a, 0, "A"))), 201);
        JsonNode second = submit(id, submissionBody(studentId,
                answer(questionId(a, 0), optionId(a, 0, "B")),
                answer(questionId(a, 1), optionId(a, 1, "X"))), 201);

        assertThat(second.get("submissionId").asLong()).isNotEqualTo(first.get("submissionId").asLong());
        // The first attempt is untouched.
        JsonNode firstAgain = get("/api/submissions/" + first.get("submissionId").asLong(), 200);
        assertThat(firstAgain.get("score").asInt()).isZero();
    }
}
