package com.example.assessment.repository;

import com.example.assessment.entity.Submission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    boolean existsByAssessmentId(Long assessmentId);

    @Override
    @EntityGraph(attributePaths = {"assessment", "student"})
    Optional<Submission> findById(Long id);

    @EntityGraph(attributePaths = {"assessment", "student"})
    List<Submission> findByStudentIdOrderBySubmittedAtDescIdDesc(Long studentId);

    @EntityGraph(attributePaths = {"assessment", "student"})
    List<Submission> findByStudentIdOrderBySubmittedAtAscIdAsc(Long studentId);
}
