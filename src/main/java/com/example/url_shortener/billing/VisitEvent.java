package com.example.url_shortener.billing;

import java.time.Instant;
import java.util.UUID;

public record VisitEvent(
        String visitId, // for idempotency ( dedupe)
        String shortCode,
        Long clientId,
        Instant timestamp
) {
    public static VisitEvent of(String shortCode , Long clientId) {
        return new VisitEvent(UUID.randomUUID().toString(), shortCode,clientId, Instant.now());
    }
}