package com.example.assessment.service;

import com.example.assessment.dto.StudentRequest;
import com.example.assessment.dto.StudentResponse;
import com.example.assessment.entity.Student;
import com.example.assessment.exception.ConflictException;
import com.example.assessment.exception.ResourceNotFoundException;
import com.example.assessment.mapper.AssessmentMapper;
import com.example.assessment.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class StudentService {

    private final StudentRepository students;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository students, PasswordEncoder passwordEncoder) {
        this.students = students;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (students.existsByEmail(email)) {
            throw new ConflictException("A student with email '" + email + "' already exists.");
        }
        Student student = new Student();
        student.setFullName(request.fullName().trim());
        student.setEmail(email);
        String password = request.password() == null || request.password().isBlank()
                ? UUID.randomUUID().toString() : request.password();
        student.setPasswordHash(passwordEncoder.encode(password));
        student.setRole("STUDENT");
        return toResponse(students.saveAndFlush(student));
    }

    @Transactional(readOnly = true)
    public StudentResponse get(Long id) {
        return toResponse(students.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student " + id + " not found")));
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> list() {
        return students.findAllByOrderByIdAsc().stream().map(StudentService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public String emailFor(Long id) {
        return students.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student " + id + " not found"))
                .getEmail();
    }

    private static StudentResponse toResponse(Student s) {
        return new StudentResponse(s.getId(), s.getFullName(), s.getEmail(),
                AssessmentMapper.toInstant(s.getCreatedAt()));
    }
}
