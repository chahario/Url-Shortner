package com.example.url_shortener.web.dto;

import java.time.LocalDate;
import java.util.List;

public record StatsResponse(
        String shortCode,
        LocalDate from,
        LocalDate to,
        long totalVisits,
        List<DailyCount> series
) {
    public record DailyCount(LocalDate day, long visits) {
    }
}