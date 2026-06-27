package com.example.url_shortener.service;

import com.example.url_shortener.domain.UsageDaily;
import com.example.url_shortener.repository.UsageDailyRepository;
import com.example.url_shortener.web.dto.StatsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class StatsService {

    private final UsageDailyRepository usageDailyRepository;

    public StatsService(UsageDailyRepository usageDailyRepository) {
        this.usageDailyRepository = usageDailyRepository;
    }

    @Transactional(readOnly = true)
    public StatsResponse getStats(String shortCode, LocalDate from, LocalDate to) {

        List<UsageDaily> rows = usageDailyRepository
                .findByIdShortCodeAndIdDayBetweenOrderByIdDay(shortCode, from, to);

        // Map each daily row to a (day, visits) entry for the series.
        List<StatsResponse.DailyCount> series = rows.stream()
                .map(r -> new StatsResponse.DailyCount(r.getId().getDay(), r.getVisits()))
                .toList();

        // Sum all daily counts -> the total for the whole window (this is how week/month rollups work).
        long total = rows.stream()
                .mapToLong(UsageDaily::getVisits)
                .sum();

        return new StatsResponse(shortCode, from, to, total, series);
    }
}