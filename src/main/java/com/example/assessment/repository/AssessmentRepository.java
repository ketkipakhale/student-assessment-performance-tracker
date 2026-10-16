package com.example.assessment.repository;

import com.example.assessment.entity.Assessment;
import com.example.assessment.entity.AssessmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findAllByOrderByIdDesc();

    Optional<Assessment> findByTitle(String title);

    List<Assessment> findAllByTitleOrderByIdAsc(String title);

    List<Assessment> findByStatusOrderByIdDesc(AssessmentStatus status);

    Optional<Assessment> findByIdAndStatus(Long id, AssessmentStatus status);
}
