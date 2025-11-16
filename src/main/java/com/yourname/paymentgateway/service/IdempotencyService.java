package com.yourname.paymentgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourname.paymentgateway.dto.response.TransactionResponse;
import com.yourname.paymentgateway.entity.IdempotencyCache;
import com.yourname.paymentgateway.repository.IdempotencyCacheRepository;
import com.yourname.paymentgateway.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    
    private final IdempotencyCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;
    
    private static final int CACHE_TTL_HOURS = 24;
    
    @Transactional(readOnly = true)
    @SneakyThrows
    public TransactionResponse getCachedResponse(Long merchantId, String idempotencyKey) {
        return cacheRepository
            .findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey)
            .filter(cache -> cache.getExpiresAt().isAfter(LocalDateTime.now()))
            .map(cache -> {
                try {
                    return objectMapper.readValue(
                        cache.getResponseBody(), 
                        TransactionResponse.class
                    );
                } catch (Exception e) {
                    return null;
                }
            })
            .orElse(null);
    }
    
    @Transactional
    @SneakyThrows
    public void cacheResponse(
        Long merchantId,
        String idempotencyKey,
        TransactionResponse response
    ) {
        String responseBody = objectMapper.writeValueAsString(response);
        String requestHash = HashUtil.sha256(responseBody);
        
        IdempotencyCache cache = IdempotencyCache.builder()
            .merchantId(merchantId)
            .idempotencyKey(idempotencyKey)
            .requestHash(requestHash)
            .responseBody(responseBody)
            .responseStatusCode(201)
            .expiresAt(LocalDateTime.now().plusHours(CACHE_TTL_HOURS))
            .build();
        
        cacheRepository.save(cache);
    }
    
    @Transactional
    public void cleanupExpiredCache() {
        cacheRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}

