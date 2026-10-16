package com.example.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Adds created/updated timestamps (stored in UTC). */
@MappedSuperclass
public abstract class AuditedEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /** Marks the entity as modified even if only its children changed. */
    public void touch() {
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
