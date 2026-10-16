package com.example.assessment.config;

import com.example.assessment.entity.Student;
import com.example.assessment.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner createConfiguredAdmin(StudentRepository students, PasswordEncoder encoder,
                                            @Value("${ADMIN_EMAIL:}") String email,
                                            @Value("${ADMIN_PASSWORD:}") String password) {
        return args -> {
            if (email.isBlank() || password.isBlank()) {
                return;
            }
            String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
            if (students.existsByEmail(normalizedEmail)) {
                return;
            }
            Student admin = new Student();
            admin.setFullName("System Administrator");
            admin.setEmail(normalizedEmail);
            admin.setPasswordHash(encoder.encode(password));
            admin.setRole("ADMIN");
            students.save(admin);
        };
    }
}
