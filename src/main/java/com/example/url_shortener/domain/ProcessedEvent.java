package com.example.url_shortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

    @Id
    @Column(name = "visit_id", length = 64, nullable = false, updatable = false)
    private String visitId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    protected ProcessedEvent() {
    }

    public ProcessedEvent(String visitId) {
        this.visitId = visitId;
        this.processedAt = Instant.now();
    }

    public String getVisitId()      { return visitId; }
    public Instant getProcessedAt() { return processedAt; }
}