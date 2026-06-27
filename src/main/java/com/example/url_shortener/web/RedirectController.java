package com.example.url_shortener.web;


import com.example.url_shortener.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {
    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        return urlService.resolveLongUrl(shortCode)
                .map(longUrl -> ResponseEntity
                        .status(HttpStatus.FOUND)
                        .location(URI.create(longUrl))
                        .<Void>build())          // ← this <Void> is the fix
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
