package com.example.assessment.controller;

import com.example.assessment.dto.StudentRequest;
import com.example.assessment.dto.StudentResponse;
import com.example.assessment.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/students")
@Tag(name = "Students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Register a student", description = "Errors: 400 validation, 409 duplicate email.")
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
        StudentResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a student by ID", description = "Errors: 404 not found.")
    public StudentResponse get(@PathVariable Long id, Authentication authentication) {
        if (authentication != null && !service.emailFor(id).equalsIgnoreCase(authentication.getName())
                && authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new org.springframework.security.access.AccessDeniedException("You can only view your own account.");
        }
        return service.get(id);
    }

    @GetMapping
    @Operation(summary = "List all students")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StudentResponse> list() {
        return service.list();
    }
}
