package com.example.assessment.controller;

import com.example.assessment.dto.AdminAssessmentResponse;
import com.example.assessment.dto.AssessmentDetailsRequest;
import com.example.assessment.dto.AssessmentRequest;
import com.example.assessment.dto.AssessmentSummaryResponse;
import com.example.assessment.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/assessments")
@Tag(name = "Admin - Assessments", description = "Manage assessments. Responses include the answer key.")
public class AdminAssessmentController {

    private final AssessmentService service;

    public AdminAssessmentController(AssessmentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create an assessment with questions and options",
            description = "Each question needs 2-10 options and exactly one correct option. "
                    + "Errors: 400 validation.")
    public ResponseEntity<AdminAssessmentResponse> create(@Valid @RequestBody AssessmentRequest request) {
        AdminAssessmentResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List all assessments (draft and published)")
    public List<AssessmentSummaryResponse> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one assessment with questions, options and answer key",
            description = "Errors: 404 if the assessment does not exist.")
    public AdminAssessmentResponse get(@PathVariable Long id) {
        return service.getForAdmin(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an assessment's details, questions and options",
            description = "Errors: 400 validation, 404 not found, 409 if students have already submitted "
                    + "(use PATCH instead). Question and option IDs change on every replace.")
    public AdminAssessmentResponse replace(@PathVariable Long id, @Valid @RequestBody AssessmentRequest request) {
        return service.replace(id, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update title, description and/or status",
            description = "Omitted fields stay unchanged. Allowed even after submissions exist, so you can "
                    + "set a published assessment back to DRAFT.")
    public AdminAssessmentResponse patch(@PathVariable Long id,
                                         @Valid @RequestBody AssessmentDetailsRequest request) {
        return service.updateDetails(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an assessment",
            description = "Returns 204. Errors: 404 not found, 409 if the assessment has submissions.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
