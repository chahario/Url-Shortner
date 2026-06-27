package com.example.url_shortener.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateUrlRequest(
        @NotBlank(message = "long_url is required!")
        @Pattern(regexp = "^https?://.+", message = "long_url must with http:// or https://")
        String longUrl,

        @Size(min = 4, max = 16, message = "custom-alias must be 4-16 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]*$",
                message = "custom_alias may only contain letters, digits, hyphen, underscore")
        String customAlias,

        Instant expiresAt
){}
