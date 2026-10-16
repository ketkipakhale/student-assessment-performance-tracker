package com.example.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultsAndProgressTest extends IntegrationTestSupport {

    @Test
    void resultsListNewestFirstAndProgressOldestFirstWithAverage() throws Exception {
        JsonNode quizOne = createAssessment("Quiz One", "PUBLISHED");
        JsonNode quizTwo = createAssessment("Quiz Two", "PUBLISHED");
        long studentId = createStudent("progress@example.com");

        // Attempt 1: 0/5 = 0%   Attempt 2: 5/5 = 100%   Attempt 3: 2/5 = 40%
        JsonNode first = submit(quizOne.get("id").asLong(), submissionBody(studentId,
                answer(questionId(quizOne, 0), optionId(quizOne, 0, "A")),
                answer(questionId(quizOne, 1), optionId(quizOne, 1, "Y"))), 201);
        JsonNode second = submit(quizTwo.get("id").asLong(), submissionBody(studentId,
                answer(questionId(quizTwo, 0), optionId(quizTwo, 0, "B")),
                answer(questionId(quizTwo, 1), optionId(quizTwo, 1, "X"))), 201);
        JsonNode third = submit(quizOne.get("id").asLong(), submissionBody(studentId,
                answer(questionId(quizOne, 0), optionId(quizOne, 0, "B"))), 201);

        JsonNode results = get("/api/students/" + studentId + "/results", 200);
        assertThat(results).hasSize(3);
        assertThat(results.get(0).get("submissionId").asLong()).isEqualTo(third.get("submissionId").asLong());
        assertThat(results.get(2).get("submissionId").asLong()).isEqualTo(first.get("submissionId").asLong());
        assertThat(results.get(0).get("assessmentTitle").asText()).isEqualTo("Quiz One");

        JsonNode progress = get("/api/students/" + studentId + "/progress", 200);
        assertThat(progress.get("attempts").asInt()).isEqualTo(3);
        assertThat(progress.get("averagePercentage").decimalValue()).isEqualByComparingTo("46.67");
        JsonNode entries = progress.get("progress");
        assertThat(entries.get(0).get("submissionId").asLong()).isEqualTo(first.get("submissionId").asLong());
        assertThat(entries.get(1).get("submissionId").asLong()).isEqualTo(second.get("submissionId").asLong());
        assertThat(entries.get(2).get("submissionId").asLong()).isEqualTo(third.get("submissionId").asLong());
        assertThat(entries.get(1).get("percentage").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(entries.get(1).get("totalPoints").asInt()).isEqualTo(5);
        assertThat(entries.get(1).get("assessmentTitle").asText()).isEqualTo("Quiz Two");
        assertThat(entries.get(1).get("submittedAt").asText()).isNotBlank();
    }

    @Test
    void studentWithNoAttemptsHasEmptyProgress() throws Exception {
        long studentId = createStudent("none@example.com");
        JsonNode progress = get("/api/students/" + studentId + "/progress", 200);
        assertThat(progress.get("attempts").asInt()).isZero();
        assertThat(progress.get("progress")).isEmpty();
        assertThat(progress.get("averagePercentage").decimalValue()).isEqualByComparingTo("0");
        assertThat(get("/api/students/" + studentId + "/results", 200)).isEmpty();
    }

    @Test
    void resultsAndProgressForUnknownStudentReturn404() throws Exception {
        get("/api/students/777777/results", 404);
        get("/api/students/777777/progress", 404);
    }

    @Test
    void submissionDetailShowsPerQuestionFeedbackWithoutRevealingTheKey() throws Exception {
        JsonNode a = createAssessment("Detail", "PUBLISHED");
        long studentId = createStudent("detail@example.com");
        JsonNode result = submit(a.get("id").asLong(), submissionBody(studentId,
                answer(questionId(a, 0), optionId(a, 0, "A")),
                answer(questionId(a, 1), optionId(a, 1, "X"))), 201);

        JsonNode detail = get("/api/submissions/" + result.get("submissionId").asLong(), 200);

        assertThat(detail.get("answers")).hasSize(2);
        assertThat(detail.get("answers").get(0).get("correct").asBoolean()).isFalse();
        assertThat(detail.get("answers").get(1).get("correct").asBoolean()).isTrue();
        assertThat(detail.toString()).doesNotContain("correctOption");
    }

    @Test
    void unknownSubmissionReturns404() throws Exception {
        get("/api/submissions/555555", 404);
    }
}
