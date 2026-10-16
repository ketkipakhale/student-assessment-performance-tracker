package com.example.assessment.service;

import com.example.assessment.dto.AdminAssessmentResponse;
import com.example.assessment.dto.AssessmentDetailsRequest;
import com.example.assessment.dto.AssessmentRequest;
import com.example.assessment.dto.AssessmentResponse;
import com.example.assessment.dto.AssessmentSummaryResponse;
import com.example.assessment.dto.OptionRequest;
import com.example.assessment.dto.QuestionRequest;
import com.example.assessment.entity.Assessment;
import com.example.assessment.entity.AssessmentStatus;
import com.example.assessment.entity.Question;
import com.example.assessment.entity.QuestionOption;
import com.example.assessment.exception.ConflictException;
import com.example.assessment.exception.InvalidRequestException;
import com.example.assessment.exception.ResourceNotFoundException;
import com.example.assessment.mapper.AssessmentMapper;
import com.example.assessment.repository.AssessmentRepository;
import com.example.assessment.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AssessmentService {

    private final AssessmentRepository assessments;
    private final SubmissionRepository submissions;

    public AssessmentService(AssessmentRepository assessments, SubmissionRepository submissions) {
        this.assessments = assessments;
        this.submissions = submissions;
    }

    // ---------------------------------------------------------------- admin operations

    @Transactional
    public AdminAssessmentResponse create(AssessmentRequest request) {
        validateStructure(request);
        Assessment assessment = new Assessment();
        applyContent(assessment, request, AssessmentStatus.DRAFT);
        return AssessmentMapper.toAdmin(assessments.saveAndFlush(assessment));
    }

    /**
     * Creates the local demo once, or repairs its questions in place when it is an old
     * unsubmitted demo. Existing submitted assessments are never rewritten.
     */
    @Transactional
    public AdminAssessmentResponse ensureDemo(AssessmentRequest request) {
        List<Assessment> duplicates = assessments.findAllByTitleOrderByIdAsc(request.title());
        Assessment existing = duplicates.stream()
                .filter(this::isValidDemo)
                .findFirst()
                .orElse(duplicates.isEmpty() ? null : duplicates.get(0));
        if (existing == null && "Java Basics Demo".equals(request.title())) {
            duplicates = assessments.findAllByTitleOrderByIdAsc("Java Basics - 10 Question Demo");
            existing = duplicates.isEmpty() ? null : duplicates.get(0);
            if (existing != null && !submissions.existsByAssessmentId(existing.getId())) {
                existing.setTitle(request.title());
            }
        }
        for (Assessment duplicate : duplicates) {
            if (duplicate != existing && request.title().equals(duplicate.getTitle())) {
                duplicate.setTitle(request.title() + " (Archived " + duplicate.getId() + ")");
                duplicate.touch();
                assessments.saveAndFlush(duplicate);
            }
        }
        if (existing == null) {
            validateStructure(request);
            Assessment assessment = new Assessment();
            applyContent(assessment, request, AssessmentStatus.PUBLISHED);
            return AssessmentMapper.toAdmin(assessments.saveAndFlush(assessment));
        }
        if (isValidDemo(existing)) {
            if (existing.getStatus() != AssessmentStatus.PUBLISHED) {
                existing.setStatus(AssessmentStatus.PUBLISHED);
                existing.touch();
                return AssessmentMapper.toAdmin(assessments.saveAndFlush(existing));
            }
            hideOtherDemoAssessments(existing);
            return AssessmentMapper.toAdmin(existing);
        }
        if (submissions.existsByAssessmentId(existing.getId())) {
            existing.setTitle(existing.getTitle() + " (Archived)");
            existing.touch();
            assessments.saveAndFlush(existing);
            validateStructure(request);
            Assessment replacement = new Assessment();
            applyContent(replacement, request, AssessmentStatus.PUBLISHED);
            return AssessmentMapper.toAdmin(assessments.saveAndFlush(replacement));
        }
        validateStructure(request);
        applyContent(existing, request, AssessmentStatus.PUBLISHED);
        existing.setStatus(AssessmentStatus.PUBLISHED);
        existing.touch();
        AdminAssessmentResponse response = AssessmentMapper.toAdmin(assessments.saveAndFlush(existing));
        hideOtherDemoAssessments(existing);
        return response;
    }

    private void hideOtherDemoAssessments(Assessment publishedDemo) {
        assessments.findAll().stream()
                .filter(assessment -> assessment.getId() != null)
                .filter(assessment -> !assessment.getId().equals(publishedDemo.getId()))
                .filter(assessment -> isDemoTitle(assessment.getTitle()))
                .filter(assessment -> assessment.getStatus() == AssessmentStatus.PUBLISHED)
                .forEach(assessment -> {
                    assessment.setStatus(AssessmentStatus.DRAFT);
                    assessment.touch();
                    assessments.saveAndFlush(assessment);
                });
    }

    private boolean isDemoTitle(String title) {
        return "Java Basics Demo".equals(title)
                || "Java Basics - 10 Question Demo".equals(title)
                || (title != null && title.startsWith("Java Basics Demo (Archived"));
    }

    @Transactional(readOnly = true)
    public List<AssessmentSummaryResponse> listAll() {
        return assessments.findAllByOrderByIdDesc().stream().map(AssessmentMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public AdminAssessmentResponse getForAdmin(Long id) {
        return AssessmentMapper.toAdmin(find(id));
    }

    /**
     * Full replace of the assessment (details + all questions/options). Not allowed once students have
     * submitted, because that would change what their saved scores refer to.
     */
    @Transactional
    public AdminAssessmentResponse replace(Long id, AssessmentRequest request) {
        Assessment assessment = find(id);
        if (submissions.existsByAssessmentId(id)) {
            throw new ConflictException("Assessment " + id + " already has submissions, so its questions and "
                    + "options can no longer be replaced. Use PATCH to change the title, description or status.");
        }
        validateStructure(request);
        applyContent(assessment, request, assessment.getStatus());
        assessment.touch();
        return AssessmentMapper.toAdmin(assessments.saveAndFlush(assessment));
    }

    /** Partial update of title/description/status. Always allowed, even after submissions exist. */
    @Transactional
    public AdminAssessmentResponse updateDetails(Long id, AssessmentDetailsRequest request) {
        Assessment assessment = find(id);
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new InvalidRequestException("title must not be blank");
            }
            assessment.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            assessment.setDescription(request.description().trim());
        }
        if (request.status() != null) {
            assessment.setStatus(request.status());
        }
        assessment.touch();
        return AssessmentMapper.toAdmin(assessments.saveAndFlush(assessment));
    }

    /** Deleting is refused (409) when submissions exist, so student history is never lost. */
    @Transactional
    public void delete(Long id) {
        Assessment assessment = find(id);
        if (submissions.existsByAssessmentId(id)) {
            throw new ConflictException("Assessment " + id + " has submissions and cannot be deleted. "
                    + "Set its status to DRAFT with PATCH to stop new submissions instead.");
        }
        assessments.delete(assessment);
        assessments.flush();
    }

    // ---------------------------------------------------------------- student-facing reads

    @Transactional(readOnly = true)
    public List<AssessmentSummaryResponse> listPublished() {
        return assessments.findByStatusOrderByIdDesc(AssessmentStatus.PUBLISHED).stream()
                .map(AssessmentMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getPublished(Long id) {
        Assessment assessment = assessments.findByIdAndStatus(id, AssessmentStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published assessment " + id + " not found"));
        return AssessmentMapper.toStudentView(assessment);
    }

    // ---------------------------------------------------------------- helpers

    private Assessment find(Long id) {
        return assessments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment " + id + " not found"));
    }

    private boolean isValidDemo(Assessment assessment) {
        return assessment.getStatus() == AssessmentStatus.PUBLISHED
                && assessment.getQuestions().size() == 10
                && assessment.getQuestions().stream().allMatch(question ->
                question.getOptions().size() == 4
                        && question.getOptions().stream().filter(QuestionOption::isCorrect).count() == 1);
    }

    /** Rules that annotations cannot express: exactly one correct option, no duplicate option text. */
    private void validateStructure(AssessmentRequest request) {
        List<QuestionRequest> questions = request.questions();
        for (int i = 0; i < questions.size(); i++) {
            QuestionRequest question = questions.get(i);
            long correctCount = question.options().stream().filter(o -> Boolean.TRUE.equals(o.correct())).count();
            if (correctCount != 1) {
                throw new InvalidRequestException("Question " + (i + 1)
                        + " must have exactly one correct option but has " + correctCount + ".");
            }
            Set<String> seen = new HashSet<>();
            for (OptionRequest option : question.options()) {
                if (!seen.add(option.text().trim().toLowerCase(Locale.ROOT))) {
                    throw new InvalidRequestException("Question " + (i + 1)
                            + " has two options with the same text: '" + option.text().trim() + "'.");
                }
            }
        }
    }

    private void applyContent(Assessment assessment, AssessmentRequest request, AssessmentStatus defaultStatus) {
        assessment.setTitle(request.title().trim());
        assessment.setDescription(request.description() == null ? null : request.description().trim());
        assessment.setStatus(request.status() == null ? defaultStatus : request.status());

        assessment.getQuestions().clear();
        int questionOrder = 1;
        for (QuestionRequest questionRequest : request.questions()) {
            Question question = new Question();
            question.setQuestionText(questionRequest.text().trim());
            question.setPoints(questionRequest.points() == null ? 1 : questionRequest.points());
            question.setSortOrder(questionOrder++);
            int optionOrder = 1;
            for (OptionRequest optionRequest : questionRequest.options()) {
                QuestionOption option = new QuestionOption();
                option.setOptionText(optionRequest.text().trim());
                option.setCorrect(Boolean.TRUE.equals(optionRequest.correct()));
                option.setSortOrder(optionOrder++);
                question.addOption(option);
            }
            assessment.addQuestion(question);
        }
    }
}
