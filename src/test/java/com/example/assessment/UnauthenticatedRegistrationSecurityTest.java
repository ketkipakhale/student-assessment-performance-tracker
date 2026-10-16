package com.example.assessment;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.security.enabled=true")
class UnauthenticatedRegistrationSecurityTest extends IntegrationTestSupport {

    @Test
    void unauthenticatedRegistrationIsPublicButStudentApiRemainsProtected() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"fullName":"Unauthenticated Student",
                                 "email":"public-registration@example.com",
                                 "password":"strong-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value("public-registration@example.com"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/assessments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"fullName":"No Csrf Student",
                                 "email":"no-csrf@example.com",
                                 "password":"strong-password"}
                                """))
                .andExpect(status().isForbidden());
    }
}
