package com.example.assessment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI assessmentOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Student Assessment and Performance Tracking API")
                .version("1.0")
                .description("""
                        **How to use this API (no authentication in this version):**

                        1. **Admin** creates an assessment with `POST /api/admin/assessments`
                           (use `"status": "PUBLISHED"`, or publish later with `PATCH`).
                        2. Create a student with `POST /api/students`.
                        3. A student browses `GET /api/assessments` and `GET /api/assessments/{id}`
                           (answer key is never included) and note the question and option IDs.
                        4. Submit answers with `POST /api/assessments/{assessmentId}/submissions`.
                           The server scores the attempt and returns the result.
                        5. View history with `GET /api/students/{studentId}/results`,
                           one attempt with `GET /api/submissions/{id}`, and trends with
                           `GET /api/students/{studentId}/progress`.

                        Errors always use the same JSON shape: `timestamp, status, error, message, path`
                        (plus `fieldErrors` for validation problems)."""));
    }
}
