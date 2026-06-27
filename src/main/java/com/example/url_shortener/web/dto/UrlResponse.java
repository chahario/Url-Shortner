package com.example.url_shortener.web.dto;


import com.example.url_shortener.domain.Url;
import java.time.Instant;

public record UrlResponse (
        String shortCode,
        String shortUrl,
        String longUrl,
        boolean isCustom,
        Instant createdAt,
        Instant expiresAt

){
    public static UrlResponse from(Url url, String baseUrl){
        return new UrlResponse(
                url.getShortCode(),
                baseUrl + "/" + url.getShortCode(),
                url.getLongUrl(),
                url.isCustom(),
                url.getCreatedAt(),
                url.getExpiresAt()
        );
    }
}
