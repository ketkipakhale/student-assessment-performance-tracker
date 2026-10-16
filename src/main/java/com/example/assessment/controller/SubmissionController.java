package com.example.assessment.controller;

import com.example.assessment.dto.SubmissionDetailResponse;
import com.example.assessment.dto.SubmissionRequest;
import com.example.assessment.dto.SubmissionResultResponse;
import com.example.assessment.service.ResultService;
import com.example.assessment.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.core.Authentication;

import java.net.URI;

@RestController
@Tag(name = "Submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final ResultService resultService;

    public SubmissionController(SubmissionService submissionService, ResultService resultService) {
        this.submissionService = submissionService;
        this.resultService = resultService;
    }

    @PostMapping("/api/assessments/{assessmentId}/submissions")
    @Operation(summary = "Submit answers for a published assessment",
            description = "The server validates the answers and calculates the score. Questions you leave out "
                    + "score 0. Retakes create new submissions. Errors: 400 invalid/duplicate question or "
                    + "option, 404 unknown assessment or student, 409 assessment not published.")
    public ResponseEntity<SubmissionResultResponse> submit(@PathVariable Long assessmentId,
                                                           @Valid @RequestBody SubmissionRequest request,
                                                           Authentication authentication) {
        SubmissionResultResponse result = submissionService.submit(assessmentId, request,
                authentication == null ? null : authentication.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/submissions/{id}")
                .buildAndExpand(result.submissionId()).toUri();
        return ResponseEntity.created(location).body(result);
    }

    @GetMapping("/api/submissions/{submissionId}")
    @Operation(summary = "Get one saved submission with per-question feedback",
            description = "Errors: 404 not found.")
    public SubmissionDetailResponse get(@PathVariable Long submissionId, Authentication authentication) {
        return authentication == null ? resultService.getSubmission(submissionId) :
                resultService.getSubmission(submissionId, authentication.getName());
    }
}
