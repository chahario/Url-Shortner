package com.example.url_shortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class UsageDailyId implements Serializable {

    @Column(name = "short_code", length = 16, nullable = false)
    private String shortCode;

    @Column(name = "day", nullable = false)
    private LocalDate day;

    protected UsageDailyId() {
    }

    public UsageDailyId(String shortCode, LocalDate day) {
        this.shortCode = shortCode;
        this.day = day;
    }

    public String getShortCode() { return shortCode; }
    public LocalDate getDay()    { return day; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsageDailyId that)) return false;
        return Objects.equals(shortCode, that.shortCode) && Objects.equals(day, that.day);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortCode, day);
    }
}