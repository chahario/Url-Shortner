package com.example.url_shortener.web.dto;

import java.util.List;

public record UrlPageResponse(
        List<UrlResponse> data,
        String nextCursor      // pass this back as ?cursor=... for the next page; null = no more
) {
}