package com.example.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.example.assessment.entity.PasswordResetToken;
import com.example.assessment.repository.PasswordResetTokenRepository;
import com.example.assessment.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetTest extends IntegrationTestSupport {

    @Autowired
    private StudentRepository students;

    @Autowired
    private PasswordResetTokenRepository tokens;

    @Test
    void resetTokenIsSingleUseAndUnknownTokensAreRejected() throws Exception {
        JsonNode registration = post("/api/auth/register", objectMapper.createObjectNode()
                .put("fullName", "Reset Student")
                .put("email", "reset@example.com")
                .put("password", "old-password"), 200);
        assertNotNull(registration.get("id"));

        post("/api/auth/forgot-password", objectMapper.createObjectNode()
                .put("email", "reset@example.com"), 200);
        JsonNode development = get("/api/auth/development-reset-link?email=reset@example.com", 200);
        String link = development.get("resetLink").asText();
        String token = link.substring(link.indexOf("resetToken=") + "resetToken=".length());

        post("/api/auth/reset-password", objectMapper.createObjectNode()
                .put("token", token)
                .put("password", "new-password")
                .put("confirmPassword", "new-password"), 200);

        JsonNode reused = post("/api/auth/reset-password", objectMapper.createObjectNode()
                .put("token", token)
                .put("password", "another-password")
                .put("confirmPassword", "another-password"), 400);
        assertTrue(reused.get("message").asText().contains("invalid or expired"));

        post("/api/auth/reset-password", objectMapper.createObjectNode()
                .put("token", "unknown-token")
                .put("password", "new-password")
                .put("confirmPassword", "new-password"), 400);
    }

    @Test
    void forgotPasswordUsesTheSameResponseForUnknownEmail() throws Exception {
        JsonNode known = post("/api/auth/forgot-password", objectMapper.createObjectNode()
                .put("email", "known@example.com"), 200);
        JsonNode unknown = post("/api/auth/forgot-password", objectMapper.createObjectNode()
                .put("email", "unknown@example.com"), 200);
        assertTrue(known.get("message").asText().equals(unknown.get("message").asText()));
    }

    @Test
    void expiredResetTokenIsRejected() throws Exception {
        post("/api/auth/register", objectMapper.createObjectNode()
                .put("fullName", "Expired Student")
                .put("email", "expired@example.com")
                .put("password", "old-password"), 200);
        PasswordResetToken token = new PasswordResetToken();
        token.setStudent(students.findByEmail("expired@example.com").orElseThrow());
        token.setTokenHash(hash("expired-token"));
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tokens.saveAndFlush(token);

        post("/api/auth/reset-password", objectMapper.createObjectNode()
                .put("token", "expired-token")
                .put("password", "new-password")
                .put("confirmPassword", "new-password"), 400);
    }

    private static String hash(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : digest) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
