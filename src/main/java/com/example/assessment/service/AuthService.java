package com.example.assessment.service;

import com.example.assessment.dto.AuthResponse;
import com.example.assessment.entity.Student;
import com.example.assessment.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final StudentRepository students;

    public AuthService(StudentRepository students) {
        this.students = students;
    }

    public AuthResponse responseFor(String email) {
        Student student = students.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalStateException("Authenticated account no longer exists"));
        return new AuthResponse(student.getId(), student.getFullName(), student.getEmail(), student.getRole());
    }
}
