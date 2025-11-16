package com.yourname.paymentgateway.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    
    @Value("${payment.rate-limit.requests-per-minute:120}")
    private int requestsPerMinute;
    
    private final Map<Long, Bucket> cache = new ConcurrentHashMap<>();
    
    /**
     * Resolves rate limit bucket for merchant.
     * Limit: configurable requests per minute (default 120).
     */
    public Bucket resolveBucket(Long merchantId) {
        return cache.computeIfAbsent(merchantId, this::createNewBucket);
    }
    
    private Bucket createNewBucket(Long merchantId) {
        Bandwidth limit = Bandwidth.classic(
            requestsPerMinute,
            Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * Checks if request is allowed for merchant.
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean tryConsume(Long merchantId) {
        Bucket bucket = resolveBucket(merchantId);
        return bucket.tryConsume(1);
    }
    
    /**
     * Gets remaining tokens for merchant.
     */
    public long getAvailableTokens(Long merchantId) {
        Bucket bucket = resolveBucket(merchantId);
        return bucket.getAvailableTokens();
    }
}

