package com.example.assessment.service;

import com.example.assessment.entity.PasswordResetToken;
import com.example.assessment.entity.Student;
import com.example.assessment.exception.InvalidTokenException;
import com.example.assessment.repository.PasswordResetTokenRepository;
import com.example.assessment.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Service
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private final StudentRepository students;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();
    private final boolean developmentMode;
    private String latestDevelopmentLink;
    private String latestDevelopmentEmail;

    public PasswordResetService(StudentRepository students, PasswordResetTokenRepository tokens,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.password-reset.dev-mode:false}") boolean developmentMode) {
        this.students = students;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.developmentMode = developmentMode;
    }

    @Transactional
    public void requestReset(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        latestDevelopmentLink = null;
        latestDevelopmentEmail = normalizedEmail;
        students.findByEmail(normalizedEmail).ifPresent(student -> {
            tokens.findByStudentIdAndUsedAtIsNull(student.getId()).forEach(token -> {
                token.setUsedAt(LocalDateTime.now());
            });
            String rawToken = randomToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setStudent(student);
            token.setTokenHash(hash(rawToken));
            token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            tokens.save(token);
            if (developmentMode) {
                latestDevelopmentLink = "/?resetToken=" + rawToken;
            }
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new InvalidTokenException("Password and confirmation do not match.");
        }
        PasswordResetToken token = tokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("This password reset link is invalid or expired."));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("This password reset link is invalid or expired.");
        }
        token.getStudent().setPasswordHash(passwordEncoder.encode(password));
        token.setUsedAt(LocalDateTime.now());
        tokens.save(token);
    }

    public String developmentLink(String email) {
        if (!developmentMode) {
            throw new InvalidTokenException("Development password reset retrieval is disabled.");
        }
        if (email == null || !email.trim().equalsIgnoreCase(latestDevelopmentEmail)) {
            return null;
        }
        return latestDevelopmentLink;
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }
}
