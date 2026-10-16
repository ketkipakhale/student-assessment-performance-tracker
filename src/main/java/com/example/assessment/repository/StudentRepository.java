package com.example.assessment.repository;

import com.example.assessment.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByEmail(String email);

    Optional<Student> findByEmail(String email);

    List<Student> findAllByOrderByIdAsc();
}
