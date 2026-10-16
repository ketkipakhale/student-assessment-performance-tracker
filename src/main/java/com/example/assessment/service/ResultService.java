package com.example.assessment.service;

import com.example.assessment.dto.ProgressEntryResponse;
import com.example.assessment.dto.ProgressResponse;
import com.example.assessment.dto.SubmissionDetailResponse;
import com.example.assessment.dto.SubmissionResultResponse;
import com.example.assessment.entity.Submission;
import com.example.assessment.exception.ResourceNotFoundException;
import com.example.assessment.mapper.ResultMapper;
import com.example.assessment.repository.StudentRepository;
import com.example.assessment.repository.SubmissionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ResultService {

    private final StudentRepository students;
    private final SubmissionRepository submissions;

    public ResultService(StudentRepository students, SubmissionRepository submissions) {
        this.students = students;
        this.submissions = submissions;
    }

    public List<SubmissionResultResponse> listResults(Long studentId) {
        requireStudent(studentId);
        return submissions.findByStudentIdOrderBySubmittedAtDescIdDesc(studentId).stream()
                .map(ResultMapper::toResult).toList();
    }

    public List<SubmissionResultResponse> listResultsForUser(Long studentId, String email) {
        requireOwner(studentId, email);
        return listResults(studentId);
    }

    public SubmissionDetailResponse getSubmission(Long submissionId, String email) {
        Submission submission = submissions.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));
        if (!submission.getStudent().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("This result belongs to another student.");
        }
        return ResultMapper.toDetail(submission);
    }

    public SubmissionDetailResponse getSubmission(Long submissionId) {
        Submission submission = submissions.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission " + submissionId + " not found"));
        return ResultMapper.toDetail(submission);
    }

    public ProgressResponse progress(Long studentId) {
        requireStudent(studentId);
        List<Submission> attempts = submissions.findByStudentIdOrderBySubmittedAtAscIdAsc(studentId);
        List<ProgressEntryResponse> entries = attempts.stream().map(ResultMapper::toProgress).toList();
        BigDecimal average = BigDecimal.ZERO.setScale(2);
        if (!attempts.isEmpty()) {
            BigDecimal sum = attempts.stream().map(Submission::getPercentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            average = sum.divide(BigDecimal.valueOf(attempts.size()), 2, RoundingMode.HALF_UP);
        }
        return new ProgressResponse(studentId, entries.size(), average, entries);
    }

    public ProgressResponse progressForUser(Long studentId, String email) {
        requireOwner(studentId, email);
        return progress(studentId);
    }

    private void requireStudent(Long studentId) {
        if (!students.existsById(studentId)) {
            throw new ResourceNotFoundException("Student " + studentId + " not found");
        }
    }

    private void requireOwner(Long studentId, String email) {
        var student = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student " + studentId + " not found"));
        if (!student.getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("You can only view your own progress.");
        }
    }
}
