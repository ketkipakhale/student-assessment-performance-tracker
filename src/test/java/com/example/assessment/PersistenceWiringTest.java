package com.example.assessment;

import com.example.assessment.repository.AssessmentRepository;
import com.example.assessment.repository.StudentRepository;
import com.example.assessment.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * If the application context starts, Hibernate has created the tables from the entity mappings.
 */
class PersistenceWiringTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AssessmentRepository assessments;

    @Autowired
    private StudentRepository students;

    @Autowired
    private SubmissionRepository submissions;

    @Test
    void allSixTablesExist() {
        for (String table : new String[]{"students", "assessments", "questions", "question_options",
                "submissions", "submission_answers"}) {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
            assertThat(count).as(table).isNotNull();
        }
    }

    @Test
    void repositoriesWorkAgainstGeneratedSchema() throws Exception {
        createAssessment("Persisted", "PUBLISHED");
        createStudent("persist@example.com");

        assertThat(assessments.count()).isEqualTo(1);
        assertThat(students.existsByEmail("persist@example.com")).isTrue();
        assertThat(submissions.count()).isZero();
        assertThat(assessments.findAll().get(0).getQuestions()).hasSize(2);
    }
}
