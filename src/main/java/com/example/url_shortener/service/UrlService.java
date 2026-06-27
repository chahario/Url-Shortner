package com.example.url_shortener.service;


import com.example.url_shortener.billing.VisitEvent;
import com.example.url_shortener.billing.VisitEventProducer;
import com.example.url_shortener.domain.Url;
import com.example.url_shortener.repository.UrlRepository;
import com.example.url_shortener.security.CurrentClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;
import java.util.List;

import java.time.Instant;
import java.util.Optional;


@Service
public class UrlService {

    private final UrlRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final CacheService cacheService;
    private final VisitEventProducer visitEventProducer;


    public UrlService(UrlRepository repository,
                      JdbcTemplate jdbcTemplate,
                      CacheService cacheService,
                      VisitEventProducer visitEventProducer) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.cacheService = cacheService;
        this.visitEventProducer = visitEventProducer;
    }

    @Transactional
    public Url create(String longUrl, String customAlias, Instant expiresAt) {

        Long clientId = CurrentClient.get().getClientId(); // who is creating this( from the auth filter)
        String shortCode;
        boolean isCustom;

        if (customAlias != null && !customAlias.isBlank()) {
            // Client chose an alias.

            // Reserved words (api, admin, ...) can't be claimed -> reject before hitting the DB.
            if (ReservedAliases.isReserved(customAlias)) {
                throw new AliasUnavailableException(customAlias);
            }

            shortCode = customAlias;
            isCustom = true;
        } else {

            // NO alias -> generate one (sequence + base 62)
            Long next = jdbcTemplate.queryForObject("select nextval('url_seq')", Long.class);
            shortCode = Base62.encode(next);
            isCustom = false;
        }

        // Use the computed isCustom flag (not a hardcoded false), so custom aliases are marked correctly.
        Url url = new Url(shortCode, longUrl, isCustom, expiresAt,clientId);

        try {
            // INSERT; flush so the DB uniqueness check happens NOW (inside this try/catch).
            return repository.saveAndFlush(url);   // return the saved entity
        } catch (DataIntegrityViolationException e) {
            // PK constraint rejected it -> the code is already taken (concurrency-safe: DB enforces it).
            throw new AliasUnavailableException(shortCode);
        }
    }

    // Read path: cache-aside
    @Transactional(readOnly = true)
    public Optional<String> resolveLongUrl(String shortCode) {

        // 1. Check the cache first.
        Optional<String> cached = cacheService.getLongUrl(shortCode);
        if (cached.isPresent()) {
            // Count the visit on a cache HIT too -> otherwise warm-cache reads wouldn't be billed.
            // cached value is "clientId|longUrl"
            String[] parts = cached.get().split("\\|", 2);
            Long clientId = parts[0].isEmpty() ? null : Long.valueOf(parts[0]);
            String longUrl = parts[1];
            visitEventProducer.publish(VisitEvent.of(shortCode, clientId));
            return Optional.of(longUrl); // HIT - no DB touch
        }

        // 2. Miss -> go to the database.
        Optional<Url> fromDb = repository.findById(shortCode)
                .filter(u -> u.getExpiresAt() == null || u.getExpiresAt().isAfter(Instant.now()));

        // 3. If found, back-fill the cache so the next read is a hit.
        fromDb.ifPresent(u -> {
            String value = (u.getClientId() == null ? "" : u.getClientId()) + "|" + u.getLongUrl();
            cacheService.putLongUrl(shortCode, value);
        });

        // 4. Count the visit if the link resolved (DB-hit path).
        fromDb.ifPresent(u -> visitEventProducer.publish(VisitEvent.of(shortCode, u.getClientId())));

        // 5. Return just the long URL (or empty if not found / expired).
        return fromDb.map(Url::getLongUrl);
    }

    @Transactional(readOnly = true)
    public List<Url> listUrls(Long clientId, Instant cursor, int limit) {
        return repository.findPage(clientId, cursor, PageRequest.of(0, limit));
    }
}