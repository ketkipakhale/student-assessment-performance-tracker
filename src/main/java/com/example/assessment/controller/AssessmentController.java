package com.example.assessment.controller;

import com.example.assessment.dto.AssessmentResponse;
import com.example.assessment.dto.AssessmentSummaryResponse;
import com.example.assessment.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assessments")
@Tag(name = "Assessments (student view)", description = "Published assessments without the answer key.")
public class AssessmentController {

    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List published assessments")
    public List<AssessmentSummaryResponse> list() {
        return service.listPublished();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a published assessment with its questions and options (no answer key)",
            description = "Errors: 404 if it does not exist or is not published.")
    public AssessmentResponse get(@PathVariable Long id) {
        return service.getPublished(id);
    }
}
