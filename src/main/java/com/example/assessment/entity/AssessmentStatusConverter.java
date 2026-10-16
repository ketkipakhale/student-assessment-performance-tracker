package com.example.assessment.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the status as plain text ("DRAFT" / "PUBLISHED") in a VARCHAR column. */
@Converter
public class AssessmentStatusConverter implements AttributeConverter<AssessmentStatus, String> {

    @Override
    public String convertToDatabaseColumn(AssessmentStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public AssessmentStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AssessmentStatus.valueOf(dbData);
    }
}
