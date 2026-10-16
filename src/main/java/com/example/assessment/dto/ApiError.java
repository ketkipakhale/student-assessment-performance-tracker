package com.example.assessment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** The one JSON shape used for every error response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       List<FieldIssue> fieldErrors) {

    public record FieldIssue(String field, String message) {
    }
}
