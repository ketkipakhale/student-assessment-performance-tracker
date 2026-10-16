package com.example.assessment;

import com.example.assessment.entity.AssessmentStatus;
import com.example.assessment.dto.AssessmentRequest;
import com.example.assessment.dto.OptionRequest;
import com.example.assessment.dto.QuestionRequest;
import com.example.assessment.repository.AssessmentRepository;
import com.example.assessment.service.AssessmentService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DemoAssessmentInitializerTest extends IntegrationTestSupport {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private AssessmentRepository assessments;

    @Test
    void demoAssessmentIsTenQuestionsAndIdempotent() throws Exception {
        AssessmentRequest request = new AssessmentRequest("Java Basics Demo",
                "A beginner-friendly introduction to Java fundamentals.", AssessmentStatus.PUBLISHED,
                javaBasicsQuestions());

        assessmentService.ensureDemo(request);
        assessmentService.ensureDemo(request);

        var demo = assessments.findByTitle("Java Basics Demo").orElseThrow();
        assertEquals(10, demo.getQuestions().size());
        assertEquals(AssessmentStatus.PUBLISHED, demo.getStatus());
        demo.getQuestions().forEach(question -> {
            assertEquals(4, question.getOptions().size());
            assertEquals(1, question.getOptions().stream().filter(option -> option.isCorrect()).count());
        });
        assertEquals(1, assessments.findAllByOrderByIdDesc().stream()
                .filter(assessment -> assessment.getTitle().equals("Java Basics Demo")).count());

        JsonNode studentView = get("/api/assessments/" + demo.getId(), 200);
        assertEquals(10, studentView.get("questions").size());
        studentView.get("questions").forEach(question -> assertEquals(4, question.get("options").size()));

        long studentId = createStudent("java-demo@example.com");
        var answers = objectMapper.createArrayNode();
        studentView.get("questions").forEach(question -> answers.add(answer(
                question.get("id").asLong(), question.get("options").get(0).get("id").asLong())));
        var submission = objectMapper.createObjectNode();
        submission.put("studentId", studentId);
        submission.set("answers", answers);
        JsonNode result = submit(demo.getId(), submission, 201);
        assertEquals(10, result.get("totalPoints").asInt());
    }

    @Test
    void cleanupKeepsOnlyThePublishedTenQuestionDemoVisible() {
        var legacy = new com.example.assessment.entity.Assessment();
        legacy.setTitle("Java Basics - 10 Question Demo");
        legacy.setDescription("Legacy demo");
        legacy.setStatus(AssessmentStatus.PUBLISHED);
        assessments.saveAndFlush(legacy);

        assessmentService.ensureDemo(new AssessmentRequest("Java Basics Demo",
                "A beginner-friendly introduction to Java fundamentals.", AssessmentStatus.PUBLISHED,
                javaBasicsQuestions()));

        assertEquals(0, assessments.findAll().stream()
                .filter(assessment -> assessment.getTitle().equals("Java Basics - 10 Question Demo"))
                .filter(assessment -> assessment.getStatus() == AssessmentStatus.PUBLISHED)
                .count());
        assertEquals(1, assessments.findByStatusOrderByIdDesc(AssessmentStatus.PUBLISHED).stream()
                .filter(assessment -> assessment.getTitle().equals("Java Basics Demo"))
                .count());
    }

    private List<QuestionRequest> javaBasicsQuestions() {
        return java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(index -> new QuestionRequest("Java question " + index, 1, List.of(
                        new OptionRequest("Correct " + index, true),
                        new OptionRequest("Wrong A " + index, false),
                        new OptionRequest("Wrong B " + index, false),
                        new OptionRequest("Wrong C " + index, false))))
                .toList();
    }
}
