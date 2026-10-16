package com.example.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudentApiTest extends IntegrationTestSupport {

    @Test
    void createsAndFetchesStudentWithNormalizedEmail() throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("fullName", "  Asha Patil ");
        body.put("email", "Asha.Patil@Example.COM");

        JsonNode created = post("/api/students", body, 201);
        JsonNode fetched = get("/api/students/" + created.get("id").asLong(), 200);

        assertThat(fetched.get("fullName").asText()).isEqualTo("Asha Patil");
        assertThat(fetched.get("email").asText()).isEqualTo("asha.patil@example.com");
    }

    @Test
    void duplicateEmailIsRejectedWithConflictEvenWithDifferentCase() throws Exception {
        createStudent("dup@example.com");
        ObjectNode body = objectMapper.createObjectNode();
        body.put("fullName", "Someone Else");
        body.put("email", "DUP@example.com");
        JsonNode error = post("/api/students", body, 409);
        assertThat(error.get("status").asInt()).isEqualTo(409);
    }

    @Test
    void invalidEmailAndBlankNameAreRejected() throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("fullName", "");
        body.put("email", "not-an-email");
        JsonNode error = post("/api/students", body, 400);
        assertThat(error.get("fieldErrors")).hasSize(2);
    }

    @Test
    void unknownStudentReturns404() throws Exception {
        get("/api/students/987654", 404);
    }
}
