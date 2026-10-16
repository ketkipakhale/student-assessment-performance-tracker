package com.example.assessment.service;

import com.example.assessment.dto.AnswerRequest;
import com.example.assessment.dto.SubmissionRequest;
import com.example.assessment.dto.SubmissionResultResponse;
import com.example.assessment.entity.Assessment;
import com.example.assessment.entity.AssessmentStatus;
import com.example.assessment.entity.Question;
import com.example.assessment.entity.QuestionOption;
import com.example.assessment.entity.Student;
import com.example.assessment.entity.Submission;
import com.example.assessment.entity.SubmissionAnswer;
import com.example.assessment.exception.ConflictException;
import com.example.assessment.exception.InvalidRequestException;
import com.example.assessment.exception.ResourceNotFoundException;
import com.example.assessment.mapper.ResultMapper;
import com.example.assessment.repository.AssessmentRepository;
import com.example.assessment.repository.StudentRepository;
import com.example.assessment.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Validates and scores a submission entirely on the server.
 *
 * <p>Rules: every question of the assessment gets one saved answer row. Questions the student did not
 * answer are stored with no selected option and earn 0 points (no negative marking), but their points still
 * count in the total. Students may retake an assessment; each attempt is a separate submission.
 */
@Service
public class SubmissionService {

    private final AssessmentRepository assessments;
    private final StudentRepository students;
    private final SubmissionRepository submissions;

    public SubmissionService(AssessmentRepository assessments, StudentRepository students,
                             SubmissionRepository submissions) {
        this.assessments = assessments;
        this.students = students;
        this.submissions = submissions;
    }

    @Transactional
    public SubmissionResultResponse submit(Long assessmentId, SubmissionRequest request, String email) {
        Assessment assessment = assessments.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment " + assessmentId + " not found"));
        Student student = email == null
                ? students.findById(request.studentId()).orElseThrow(() ->
                new ResourceNotFoundException("Student " + request.studentId() + " not found"))
                : students.findByEmail(email.toLowerCase(Locale.ROOT)).orElseThrow(() ->
                new ResourceNotFoundException("Authenticated student not found"));
        if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
            throw new ConflictException("Assessment " + assessmentId + " is not published, so it cannot be submitted.");
        }

        Map<Long, Question> questionsById = new HashMap<>();
        for (Question question : assessment.getQuestions()) {
            questionsById.put(question.getId(), question);
        }

        // Validate the answers and remember the chosen option per question.
        Map<Long, QuestionOption> chosenByQuestion = new HashMap<>();
        for (AnswerRequest answer : request.answers()) {
            Question question = questionsById.get(answer.questionId());
            if (question == null) {
                throw new InvalidRequestException("Question " + answer.questionId()
                        + " does not belong to assessment " + assessmentId + ".");
            }
            if (chosenByQuestion.containsKey(question.getId())) {
                throw new InvalidRequestException("Duplicate answer for question " + question.getId() + ".");
            }
            QuestionOption chosen = question.getOptions().stream()
                    .filter(o -> o.getId().equals(answer.selectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new InvalidRequestException("Option " + answer.selectedOptionId()
                            + " does not belong to question " + question.getId() + "."));
            chosenByQuestion.put(question.getId(), chosen);
        }

        // Score using the stored answer key only.
        Submission submission = new Submission();
        submission.setStudent(student);
        submission.setAssessment(assessment);
        int score = 0;
        int totalPoints = 0;
        for (Question question : assessment.getQuestions()) {
            totalPoints += question.getPoints();
            QuestionOption chosen = chosenByQuestion.get(question.getId());

            SubmissionAnswer row = new SubmissionAnswer();
            row.setQuestion(question);
            row.setSelectedOption(chosen);
            boolean correct = chosen != null && chosen.isCorrect();
            row.setCorrect(correct);
            row.setPointsAwarded(correct ? question.getPoints() : 0);
            score += row.getPointsAwarded();
            submission.addAnswer(row);
        }
        submission.setScore(score);
        submission.setTotalPoints(totalPoints);
        submission.setPercentage(percentage(score, totalPoints));

        return ResultMapper.toResult(submissions.saveAndFlush(submission));
    }

    static BigDecimal percentage(int score, int totalPoints) {
        if (totalPoints <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(score).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalPoints), 2, RoundingMode.HALF_UP);
    }
}
