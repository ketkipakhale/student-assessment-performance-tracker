package com.example.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentManagementTest extends IntegrationTestSupport {

    @Test
    void createsAssessmentAndAdminViewShowsAnswerKey() throws Exception {
        JsonNode created = createAssessment("Java Basics", "PUBLISHED");

        assertThat(created.get("id").asLong()).isPositive();
        assertThat(created.get("totalPoints").asInt()).isEqualTo(5);
        assertThat(created.get("status").asText()).isEqualTo("PUBLISHED");
        assertThat(created.get("questions")).hasSize(2);
        // Admin view includes the answer key.
        assertThat(created.get("questions").get(0).get("options").get(1).get("correct").asBoolean()).isTrue();
    }

    @Test
    void statusDefaultsToDraft() throws Exception {
        JsonNode created = createAssessment("No status given", null);
        assertThat(created.get("status").asText()).isEqualTo("DRAFT");
    }

    @Test
    void rejectsBlankTitleAndMissingQuestionsWithFieldErrors() throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("title", "  ");
        body.putArray("questions");

        JsonNode error = post("/api/admin/assessments", body, 400);

        assertThat(error.get("status").asInt()).isEqualTo(400);
        assertThat(error.get("fieldErrors")).isNotEmpty();
        assertThat(error.get("path").asText()).isEqualTo("/api/admin/assessments");
    }

    @Test
    void rejectsQuestionWithFewerThanTwoOptions() throws Exception {
        ObjectNode body = assessment("Bad", "DRAFT", question("Only one option?", 1, option("A", true)));
        JsonNode error = post("/api/admin/assessments", body, 400);
        assertThat(error.get("fieldErrors").toString()).contains("questions[0].options");
    }

    @Test
    void rejectsQuestionWithNoCorrectOption() throws Exception {
        ObjectNode body = assessment("Bad", "DRAFT",
                question("No right answer?", 1, option("A", false), option("B", false)));
        JsonNode error = post("/api/admin/assessments", body, 400);
        assertThat(error.get("message").asText()).contains("exactly one correct option");
    }

    @Test
    void rejectsQuestionWithTwoCorrectOptions() throws Exception {
        ObjectNode body = assessment("Bad", "DRAFT",
                question("Two right answers?", 1, option("A", true), option("B", true)));
        JsonNode error = post("/api/admin/assessments", body, 400);
        assertThat(error.get("message").asText()).contains("exactly one correct option");
    }

    @Test
    void rejectsUnknownStatusValue() throws Exception {
        ObjectNode body = standardAssessment("Bad status", "ARCHIVED");
        post("/api/admin/assessments", body, 400);
    }

    @Test
    void getUnknownAssessmentReturns404WithConsistentErrorShape() throws Exception {
        JsonNode error = get("/api/admin/assessments/999999", 404);
        assertThat(error.get("status").asInt()).isEqualTo(404);
        assertThat(error.get("error").asText()).isEqualTo("Not Found");
        assertThat(error.get("message").asText()).contains("999999");
    }

    @Test
    void replaceUpdatesQuestionsWhenNoSubmissionsExist() throws Exception {
        JsonNode created = createAssessment("Original", "DRAFT");
        long id = created.get("id").asLong();

        ObjectNode replacement = assessment("Renamed", "DRAFT",
                question("Only question?", 4, option("Yes", true), option("No", false)));
        JsonNode updated = put("/api/admin/assessments/" + id, replacement, 200);

        assertThat(updated.get("title").asText()).isEqualTo("Renamed");
        assertThat(updated.get("questions")).hasSize(1);
        assertThat(updated.get("totalPoints").asInt()).isEqualTo(4);
    }

    @Test
    void deleteWithoutSubmissionsReturns204ThenNotFound() throws Exception {
        long id = createAssessment("To delete", "DRAFT").get("id").asLong();
        delete("/api/admin/assessments/" + id, 204);
        get("/api/admin/assessments/" + id, 404);
    }

    @Test
    void assessmentWithSubmissionsCannotBeDeletedOrReplacedButCanBeUnpublished() throws Exception {
        JsonNode created = createAssessment("Has submissions", "PUBLISHED");
        long id = created.get("id").asLong();
        long studentId = createStudent("policy@example.com");
        submit(id, submissionBody(studentId,
                answer(questionId(created, 0), optionId(created, 0, "B"))), 201);

        delete("/api/admin/assessments/" + id, 409);
        put("/api/admin/assessments/" + id, standardAssessment("Changed", "PUBLISHED"), 409);

        ObjectNode details = objectMapper.createObjectNode();
        details.put("status", "DRAFT");
        JsonNode patched = patch("/api/admin/assessments/" + id, details, 200);
        assertThat(patched.get("status").asText()).isEqualTo("DRAFT");
        assertThat(patched.get("title").asText()).isEqualTo("Has submissions");
    }
}
