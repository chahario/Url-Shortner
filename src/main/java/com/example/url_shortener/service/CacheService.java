package com.example.url_shortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class CacheService {

    private static final Logger LOG = LoggerFactory.getLogger(CacheService.class);
    private static final String KEY_PREFIX = "url:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;
    public CacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<String> getLongUrl(String shortCode){
        try{
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + shortCode);
            return Optional.ofNullable(value);
        }catch(Exception e){
        LOG.warn("Redis GET Failed for {} - treating as a cache miss", shortCode,e);
        return Optional.empty(); //  failure looks like a miss -> fall through to DB.
        }
    }

    public void putLongUrl(String shortCode, String longUrl){
        try{
        redisTemplate.opsForValue().set(KEY_PREFIX + shortCode, longUrl, TTL);

        }
        catch (Exception e){
            LOG.warn("Redis SET Failed for {} - skipping cache miss", shortCode,e);
            // not fatal : the value is safely in Postgres; we just can't cache it
        }
    }

    public void evict(String shortCode){
        try{
        redisTemplate.delete(KEY_PREFIX + shortCode);
        }
        catch (Exception e){
            LOG.warn("Redis DELETE failed for {}", shortCode, e);}
    }

}
