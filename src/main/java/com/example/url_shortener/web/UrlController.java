package com.example.url_shortener.web;

import com.example.url_shortener.domain.Url;
import com.example.url_shortener.service.StatsService;
import com.example.url_shortener.service.UrlService;
import com.example.url_shortener.web.dto.CreateUrlRequest;
import com.example.url_shortener.web.dto.UrlResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.url_shortener.web.dto.StatsResponse;
import org.springframework.format.annotation.DateTimeFormat;
import com.example.url_shortener.security.CurrentClient;
import com.example.url_shortener.web.dto.UrlPageResponse;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {
    private final UrlService urlService;
    private final String baseUrl;
    private final StatsService statsService;

    public UrlController(UrlService urlService,
                         StatsService statsService,
                         @Value("${app.base-url}") String baseUrl) {
        this.urlService = urlService;
        this.baseUrl = baseUrl;
        this.statsService = statsService;
    }

    @PostMapping
    public ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        Url url = urlService.create(request.longUrl(), request.customAlias(), request.expiresAt());
        UrlResponse response = UrlResponse.from(url, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}/stats")
    public ResponseEntity<StatsResponse> stats(
            @PathVariable String shortCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(statsService.getStats(shortCode, from, to));
    }

    @GetMapping
    public ResponseEntity<UrlPageResponse> list(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String cursor) {

        Long clientId = CurrentClient.get().getClientId();

        // Decode the opaque cursor (an ISO timestamp) -> Instant, or null for the first page.
        Instant cursorTime = (cursor == null || cursor.isBlank())
                ? null
                : Instant.parse(new String(Base64.getUrlDecoder().decode(cursor)));

        int safeLimit = Math.min(Math.max(limit, 1), 100);   // clamp 1..100
        List<Url> urls = urlService.listUrls(clientId, cursorTime, safeLimit);

        List<UrlResponse> data = urls.stream()
                .map(u -> UrlResponse.from(u, baseUrl))
                .toList();

        // Build the next cursor from the last item's createdAt (or null if this was the last page).
        String nextCursor = null;
        if (urls.size() == safeLimit) {
            Instant last = urls.get(urls.size() - 1).getCreatedAt();
            nextCursor = Base64.getUrlEncoder().encodeToString(last.toString().getBytes());
        }

        return ResponseEntity.ok(new UrlPageResponse(data, nextCursor));
    }
}
