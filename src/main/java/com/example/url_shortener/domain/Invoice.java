package com.example.url_shortener.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "invoice",
        uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "period"}))
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "period", nullable = false)
    private LocalDate period;                 // first day of the billed month, e.g. 2026-06-01

    @Column(name = "total_visits", nullable = false)
    private long totalVisits;

    @Column(name = "rate_applied", nullable = false, precision = 10, scale = 4)
    private BigDecimal rateApplied;           // SNAPSHOT of the plan rate at invoice time

    @Column(name = "amount_due", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Invoice() {
    }

    public Invoice(Long clientId, LocalDate period, long totalVisits,
                   BigDecimal rateApplied, BigDecimal amountDue) {
        this.clientId = clientId;
        this.period = period;
        this.totalVisits = totalVisits;
        this.rateApplied = rateApplied;
        this.amountDue = amountDue;
        this.createdAt = Instant.now();
    }

    public Long getInvoiceId()       { return invoiceId; }
    public Long getClientId()        { return clientId; }
    public LocalDate getPeriod()     { return period; }
    public long getTotalVisits()     { return totalVisits; }
    public BigDecimal getRateApplied() { return rateApplied; }
    public BigDecimal getAmountDue()   { return amountDue; }
    public Instant getCreatedAt()    { return createdAt; }
}