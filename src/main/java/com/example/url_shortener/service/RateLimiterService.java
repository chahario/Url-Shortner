package com.example.url_shortener.service;


import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


@Service
public class RateLimiterService {
    private static final DateTimeFormatter MINUTE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);
    private final RedisTemplate<String,String> redisTemplate;
    public RateLimiterService(RedisTemplate<String,String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns true if the request is allowed, false if the client has exceeded its limit.
     */

    public boolean isAllowed(Long clientId, int limitPerMinute){
        String minute = MINUTE_FORMAT.format(Instant.now());
        String key = "rate:" + clientId + ":" + minute;

        try{
            Long count =  redisTemplate.opsForValue().increment(key); // atomic INCR , return new value
            if (count != null && count == 1L){
                // first request this minute -> set the key to expire after 60s
                redisTemplate.expire(key,Duration.ofSeconds(60));
            }

            return count != null && count <= limitPerMinute;

        } catch (Exception e) {
            // Fail OPEN: if Redis is down, don't block requests (availability over strict limiting).
            return true;
        }
    }
}
