package com.example.assessment.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(name = "question_text", nullable = false, length = 2000)
    private String questionText;

    @Column(name = "points", nullable = false)
    private int points = 1;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<QuestionOption> options = new ArrayList<>();

    public void addOption(QuestionOption option) {
        options.add(option);
        option.setQuestion(this);
    }

    public Long getId() {
        return id;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public void setAssessment(Assessment assessment) {
        this.assessment = assessment;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<QuestionOption> getOptions() {
        return options;
    }
}
