package com.example.assessment.dto;

/** Admin-only: includes the answer key. */
public record AdminOptionResponse(Long id, String text, boolean correct) {
}
