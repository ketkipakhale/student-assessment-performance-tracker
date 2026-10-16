package com.example.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudentFacingAssessmentTest extends IntegrationTestSupport {

    @Test
    void studentViewNeverContainsAnswerKey() throws Exception {
        JsonNode created = createAssessment("Secret key", "PUBLISHED");
        long id = created.get("id").asLong();

        String raw = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/assessments/" + id))
                .andReturn().getResponse().getContentAsString();
        JsonNode view = objectMapper.readTree(raw);

        // Structure is present...
        assertThat(view.get("questions")).hasSize(2);
        assertThat(view.get("questions").get(0).get("options")).hasSize(3);
        // ...but no correctness information of any kind, anywhere in the JSON.
        assertThat(raw.toLowerCase()).doesNotContain("correct");
        for (JsonNode question : view.get("questions")) {
            for (JsonNode option : question.get("options")) {
                assertThat(option.fieldNames()).toIterable().containsExactlyInAnyOrder("id", "text");
            }
        }
    }

    @Test
    void publishedListContainsOnlyPublishedAssessmentsAndNoAnswerKey() throws Exception {
        createAssessment("Visible", "PUBLISHED");
        createAssessment("Hidden draft", "DRAFT");

        JsonNode list = get("/api/assessments", 200);

        assertThat(list.toString()).contains("Visible").doesNotContain("Hidden draft");
        assertThat(list.toString().toLowerCase()).doesNotContain("correct");
    }

    @Test
    void draftAssessmentIsNotVisibleToStudents() throws Exception {
        long id = createAssessment("Draft", "DRAFT").get("id").asLong();
        get("/api/assessments/" + id, 404);
    }
}
