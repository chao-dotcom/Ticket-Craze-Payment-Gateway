package com.yourname.paymentgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourname.paymentgateway.dto.response.TransactionResponse;
import com.yourname.paymentgateway.entity.IdempotencyCache;
import com.yourname.paymentgateway.repository.IdempotencyCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {
    
    @Mock
    private IdempotencyCacheRepository cacheRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private IdempotencyService idempotencyService;
    
    private TransactionResponse testResponse;
    
    @BeforeEach
    void setUp() {
        testResponse = TransactionResponse.builder()
            .transactionId(UUID.randomUUID())
            .amount(new BigDecimal("100.00"))
            .build();
    }
    
    @Test
    void getCachedResponse_WithValidCache_ShouldReturnResponse() throws Exception {
        // Arrange
        Long merchantId = 1L;
        String idempotencyKey = "test-key";
        
        IdempotencyCache cache = IdempotencyCache.builder()
            .idempotencyKey(idempotencyKey)
            .merchantId(merchantId)
            .responseBody("{\"transactionId\":\"test\"}")
            .expiresAt(LocalDateTime.now().plusHours(1))
            .build();
        
        when(cacheRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey))
            .thenReturn(Optional.of(cache));
        when(objectMapper.readValue(anyString(), eq(TransactionResponse.class)))
            .thenReturn(testResponse);
        
        // Act
        var result = idempotencyService.getCachedResponse(merchantId, idempotencyKey);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testResponse);
    }
    
    @Test
    void getCachedResponse_WithExpiredCache_ShouldReturnNull() {
        // Arrange
        Long merchantId = 1L;
        String idempotencyKey = "expired-key";
        
        IdempotencyCache cache = IdempotencyCache.builder()
            .idempotencyKey(idempotencyKey)
            .merchantId(merchantId)
            .expiresAt(LocalDateTime.now().minusHours(1))
            .build();
        
        when(cacheRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey))
            .thenReturn(Optional.of(cache));
        
        // Act
        var result = idempotencyService.getCachedResponse(merchantId, idempotencyKey);
        
        // Assert
        assertThat(result).isNull();
    }
    
    @Test
    void cacheResponse_ShouldSaveToRepository() throws Exception {
        // Arrange
        Long merchantId = 1L;
        String idempotencyKey = "test-key";
        String jsonResponse = "{\"transactionId\":\"test\"}";
        
        when(objectMapper.writeValueAsString(any())).thenReturn(jsonResponse);
        
        // Act
        idempotencyService.cacheResponse(merchantId, idempotencyKey, testResponse);
        
        // Assert
        verify(cacheRepository).save(any(IdempotencyCache.class));
    }
}

