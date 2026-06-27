package com.example.url_shortener.repository;

import com.example.url_shortener.domain.UsageDaily;
import com.example.url_shortener.domain.UsageDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface UsageDailyRepository extends JpaRepository<UsageDaily, UsageDailyId> {

    List<UsageDaily> findByIdShortCodeAndIdDayBetweenOrderByIdDay(
            String shortCode, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(u.visits), 0) FROM UsageDaily u " +
            "WHERE u.clientId = :clientId AND u.id.day BETWEEN :from AND :to")
    long sumVisits(@Param("clientId") Long clientId,
                   @Param("from") LocalDate from,
                   @Param("to") LocalDate to);
}