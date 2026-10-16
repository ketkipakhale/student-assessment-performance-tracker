package com.example.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class: boots the real application against an in-memory H2 database. Each test runs in a transaction
 * that is rolled back, so tests do not affect each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // ------------------------------------------------------------ HTTP helpers

    protected JsonNode send(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String body = mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return body.isBlank() ? null : objectMapper.readTree(body);
    }

    protected JsonNode get(String url, int expectedStatus) throws Exception {
        return send(MockMvcRequestBuilders.get(url), expectedStatus);
    }

    protected JsonNode post(String url, Object body, int expectedStatus) throws Exception {
        return send(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)), expectedStatus);
    }

    protected JsonNode put(String url, Object body, int expectedStatus) throws Exception {
        return send(MockMvcRequestBuilders.put(url).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)), expectedStatus);
    }

    protected JsonNode patch(String url, Object body, int expectedStatus) throws Exception {
        return send(MockMvcRequestBuilders.patch(url).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)), expectedStatus);
    }

    protected JsonNode delete(String url, int expectedStatus) throws Exception {
        return send(MockMvcRequestBuilders.delete(url), expectedStatus);
    }

    // ------------------------------------------------------------ request builders

    protected ObjectNode option(String text, boolean correct) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("text", text);
        node.put("correct", correct);
        return node;
    }

    protected ObjectNode question(String text, int points, ObjectNode... options) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("text", text);
        node.put("points", points);
        ArrayNode array = node.putArray("options");
        for (ObjectNode option : options) {
            array.add(option);
        }
        return node;
    }

    protected ObjectNode assessment(String title, String status, ObjectNode... questions) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("title", title);
        node.put("description", "Test assessment");
        if (status != null) {
            node.put("status", status);
        }
        ArrayNode array = node.putArray("questions");
        for (ObjectNode question : questions) {
            array.add(question);
        }
        return node;
    }

    /**
     * A standard 5-point assessment: Q1 (2 pts, correct = "B"), Q2 (3 pts, correct = "X").
     */
    protected ObjectNode standardAssessment(String title, String status) {
        return assessment(title, status,
                question("Question one?", 2, option("A", false), option("B", true), option("C", false)),
                question("Question two?", 3, option("X", true), option("Y", false)));
    }

    // ------------------------------------------------------------ fixtures

    protected JsonNode createAssessment(String title, String status) throws Exception {
        return post("/api/admin/assessments", standardAssessment(title, status), 201);
    }

    protected long createStudent(String email) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("fullName", "Test Student");
        body.put("email", email);
        return post("/api/students", body, 201).get("id").asLong();
    }

    /** Finds the option ID with the given text inside question index {@code q} of an admin response. */
    protected long optionId(JsonNode adminAssessment, int q, String text) {
        for (JsonNode option : adminAssessment.get("questions").get(q).get("options")) {
            if (option.get("text").asText().equals(text)) {
                return option.get("id").asLong();
            }
        }
        throw new IllegalArgumentException("No option '" + text + "' in question " + q);
    }

    protected long questionId(JsonNode adminAssessment, int q) {
        return adminAssessment.get("questions").get(q).get("id").asLong();
    }

    protected ObjectNode answer(long questionId, long optionId) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("questionId", questionId);
        node.put("selectedOptionId", optionId);
        return node;
    }

    protected ObjectNode submissionBody(long studentId, ObjectNode... answers) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("studentId", studentId);
        ArrayNode array = node.putArray("answers");
        for (ObjectNode answer : answers) {
            array.add(answer);
        }
        return node;
    }

    protected JsonNode submit(long assessmentId, ObjectNode body, int expectedStatus) throws Exception {
        return post("/api/assessments/" + assessmentId + "/submissions", body, expectedStatus);
    }
}
