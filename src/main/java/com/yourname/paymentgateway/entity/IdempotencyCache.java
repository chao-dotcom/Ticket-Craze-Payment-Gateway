package com.yourname.paymentgateway.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_cache", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key"),
    @Index(name = "idx_idempotency_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyCache {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idempotency_key", unique = true, nullable = false, length = 255)
    private String idempotencyKey;
    
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;
    
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    
    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(name = "response_status_code", nullable = false)
    private Integer responseStatusCode;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}

