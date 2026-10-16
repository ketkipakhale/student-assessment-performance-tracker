package com.example.assessment;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthApiTest extends IntegrationTestSupport {

    @Test
    void returnsUpStatus() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"status":"UP"}
                        """));
    }
}
