package com.example.assessment.controller;

import com.example.assessment.dto.AuthResponse;
import com.example.assessment.dto.LoginRequest;
import com.example.assessment.dto.StudentRequest;
import com.example.assessment.dto.RegisterRequest;
import com.example.assessment.dto.ForgotPasswordRequest;
import com.example.assessment.dto.ResetPasswordRequest;
import com.example.assessment.dto.PasswordResetDevelopmentResponse;
import com.example.assessment.service.AuthService;
import com.example.assessment.service.PasswordResetService;
import com.example.assessment.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final StudentService students;
    private final AuthService auth;
    private final PasswordResetService passwordReset;
    private final String applicationUrl;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          StudentService students, AuthService auth, PasswordResetService passwordReset,
                          @Value("${app.public-url:http://localhost:8080}") String applicationUrl) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.students = students;
        this.auth = auth;
        this.passwordReset = passwordReset;
        this.applicationUrl = applicationUrl;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.responseFor(students.create(new StudentRequest(request.fullName(), request.email(),
                request.password())).email());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim().toLowerCase(),
                        request.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return auth.responseFor(authentication.getName());
    }

    @GetMapping("/me")
    public AuthResponse me(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return auth.responseFor(authentication.getName());
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordReset.requestReset(request.email());
        return Map.of("message", "If an account exists for that email, password reset instructions are available.");
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordReset.resetPassword(request.token(), request.password(), request.confirmPassword());
        return Map.of("message", "Your password has been reset. You can now sign in.");
    }

    @GetMapping("/development-reset-link")
    public PasswordResetDevelopmentResponse developmentResetLink(
            @org.springframework.web.bind.annotation.RequestParam String email) {
        String link = passwordReset.developmentLink(email);
        return new PasswordResetDevelopmentResponse(
                link == null ? null : applicationUrl + link,
                "Development-only local reset retrieval. Disable app.password-reset.dev-mode outside local development.");
    }
}
