package com.example.assessment.controller;

import com.example.assessment.dto.ProgressResponse;
import com.example.assessment.dto.SubmissionResultResponse;
import com.example.assessment.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/students/{studentId}")
@Tag(name = "Results and progress")
public class ResultController {

    private final ResultService service;

    public ResultController(ResultService service) {
        this.service = service;
    }

    @GetMapping("/results")
    @Operation(summary = "List a student's results, newest first", description = "Errors: 404 unknown student.")
    public List<SubmissionResultResponse> results(@PathVariable Long studentId, Authentication authentication) {
        return authentication == null ? service.listResults(studentId) :
                service.listResultsForUser(studentId, authentication.getName());
    }

    @GetMapping("/progress")
    @Operation(summary = "Progress over time: all attempts oldest first, plus the average percentage",
            description = "Errors: 404 unknown student.")
    public ProgressResponse progress(@PathVariable Long studentId, Authentication authentication) {
        return authentication == null ? service.progress(studentId) :
                service.progressForUser(studentId, authentication.getName());
    }
}
