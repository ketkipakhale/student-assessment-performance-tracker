package com.example.assessment.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessments")
public class Assessment extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Convert(converter = AssessmentStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private AssessmentStatus status = AssessmentStatus.DRAFT;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<Question> questions = new ArrayList<>();

    public void addQuestion(Question question) {
        questions.add(question);
        question.setAssessment(this);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AssessmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentStatus status) {
        this.status = status;
    }

    public List<Question> getQuestions() {
        return questions;
    }
}
