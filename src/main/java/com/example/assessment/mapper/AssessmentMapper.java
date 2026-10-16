package com.example.assessment.mapper;

import com.example.assessment.dto.AdminAssessmentResponse;
import com.example.assessment.dto.AdminOptionResponse;
import com.example.assessment.dto.AdminQuestionResponse;
import com.example.assessment.dto.AssessmentResponse;
import com.example.assessment.dto.AssessmentSummaryResponse;
import com.example.assessment.dto.OptionResponse;
import com.example.assessment.dto.QuestionResponse;
import com.example.assessment.entity.Assessment;
import com.example.assessment.entity.Question;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Converts entities to DTOs. Student-facing methods never touch the answer key. */
public final class AssessmentMapper {

    private AssessmentMapper() {
    }

    public static Instant toInstant(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }

    public static int totalPoints(Assessment a) {
        return a.getQuestions().stream().mapToInt(Question::getPoints).sum();
    }

    public static AdminAssessmentResponse toAdmin(Assessment a) {
        return new AdminAssessmentResponse(
                a.getId(), a.getTitle(), a.getDescription(), a.getStatus(), totalPoints(a),
                toInstant(a.getCreatedAt()), toInstant(a.getUpdatedAt()),
                a.getQuestions().stream()
                        .map(q -> new AdminQuestionResponse(q.getId(), q.getQuestionText(), q.getPoints(),
                                q.getSortOrder(),
                                q.getOptions().stream()
                                        .map(o -> new AdminOptionResponse(o.getId(), o.getOptionText(),
                                                o.isCorrect()))
                                        .toList()))
                        .toList());
    }

    public static AssessmentSummaryResponse toSummary(Assessment a) {
        return new AssessmentSummaryResponse(a.getId(), a.getTitle(), a.getDescription(), a.getStatus(),
                a.getQuestions().size(), totalPoints(a));
    }

    /** Student-facing: options carry only id and text. */
    public static AssessmentResponse toStudentView(Assessment a) {
        return new AssessmentResponse(
                a.getId(), a.getTitle(), a.getDescription(), totalPoints(a),
                a.getQuestions().stream()
                        .map(q -> new QuestionResponse(q.getId(), q.getQuestionText(), q.getPoints(),
                                q.getSortOrder(),
                                q.getOptions().stream()
                                        .map(o -> new OptionResponse(o.getId(), o.getOptionText()))
                                        .toList()))
                        .toList());
    }
}
