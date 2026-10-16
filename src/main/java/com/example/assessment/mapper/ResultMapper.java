package com.example.assessment.mapper;

import com.example.assessment.dto.AnswerResultResponse;
import com.example.assessment.dto.ProgressEntryResponse;
import com.example.assessment.dto.SubmissionDetailResponse;
import com.example.assessment.dto.SubmissionResultResponse;
import com.example.assessment.entity.Submission;
import com.example.assessment.entity.SubmissionAnswer;

import java.util.List;

public final class ResultMapper {

    private ResultMapper() {
    }

    public static SubmissionResultResponse toResult(Submission s) {
        return new SubmissionResultResponse(s.getId(), s.getStudent().getId(), s.getAssessment().getId(),
                s.getAssessment().getTitle(), s.getScore(), s.getTotalPoints(), s.getPercentage(),
                AssessmentMapper.toInstant(s.getSubmittedAt()));
    }

    public static SubmissionDetailResponse toDetail(Submission s) {
        List<AnswerResultResponse> answers = s.getAnswers().stream().map(ResultMapper::toAnswer).toList();
        int answered = (int) answers.stream().filter(AnswerResultResponse::answered).count();
        return new SubmissionDetailResponse(s.getId(), s.getStudent().getId(), s.getAssessment().getId(),
                s.getAssessment().getTitle(), s.getScore(), s.getTotalPoints(), s.getPercentage(),
                AssessmentMapper.toInstant(s.getSubmittedAt()), answered, answers.size() - answered, answers);
    }

    public static ProgressEntryResponse toProgress(Submission s) {
        return new ProgressEntryResponse(s.getId(), s.getAssessment().getId(), s.getAssessment().getTitle(),
                s.getScore(), s.getTotalPoints(), s.getPercentage(),
                AssessmentMapper.toInstant(s.getSubmittedAt()));
    }

    private static AnswerResultResponse toAnswer(SubmissionAnswer a) {
        boolean answered = a.getSelectedOption() != null;
        return new AnswerResultResponse(a.getQuestion().getId(), a.getQuestion().getQuestionText(),
                answered ? a.getSelectedOption().getId() : null, answered, a.isCorrect(),
                a.getPointsAwarded(), a.getQuestion().getPoints());
    }
}
