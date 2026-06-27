package com.example.url_shortener.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "plan")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;                       // 'free', 'pro', 'enterprise'

    @Column(name = "rate_per_visit", nullable = false, precision = 10, scale = 4)
    private BigDecimal ratePerVisit;           // money charged per redirect

    @Column(name = "rate_limit_per_minute", nullable = false)
    private int rateLimitPerMinute;            // how many create-calls/min this tier allows

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Plan() {
    }

    public Plan(String name, BigDecimal ratePerVisit, int rateLimitPerMinute) {
        this.name = name;
        this.ratePerVisit = ratePerVisit;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public Long getPlanId()             { return planId; }
    public String getName()             { return name; }
    public BigDecimal getRatePerVisit() { return ratePerVisit; }
    public int getRateLimitPerMinute()  { return rateLimitPerMinute; }
    public boolean isActive()           { return active; }
}