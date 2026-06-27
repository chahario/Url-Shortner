package com.example.url_shortener.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "url")
public class Url {

    @Id
    @Column(name = "short_code", length = 16, nullable = false, updatable = false)
    private String shortCode;

    @Column(name = "long_url", nullable = false, columnDefinition = "text")
    private String longUrl;

    @Column(name = "is_custom", nullable = false)
    private boolean custom = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "client_id")
    private Long clientId;        // owner of this short URL (for billing attribution)

    protected Url() {}

    public Url(String shortCode, String longUrl, boolean custom, Instant expiresAt, Long clientId) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.custom = custom;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.clientId = clientId;

    }

    public String getShortCode()  { return shortCode; }
    public String getLongUrl()    { return longUrl; }
    public boolean isCustom()     { return custom; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Long getClientId() { return clientId; }

}
