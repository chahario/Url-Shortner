package com.example.url_shortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "usage_daily")
public class UsageDaily {

    @EmbeddedId
    private UsageDailyId id;

    @Column(name = "client_id")
    private Long clientId;        // nullable for now; clients arrive in a later slice

    @Column(name = "visits", nullable = false)
    private long visits = 0;

    protected UsageDaily() {
    }

    public UsageDaily(UsageDailyId id, long visits) {
        this.id = id;
        this.visits = visits;
    }

    public UsageDailyId getId() { return id; }
    public Long getClientId()   { return clientId; }
    public long getVisits()     { return visits; }

    public void setVisits(long visits) { this.visits = visits; }
    public void addVisits(long delta)  { this.visits += delta; }

    public void setClientId(Long aLong) {
    }
}