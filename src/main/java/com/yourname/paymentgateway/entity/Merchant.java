package com.yourname.paymentgateway.entity;

import com.yourname.paymentgateway.enums.MerchantStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchants", indexes = {
    @Index(name = "idx_merchant_code", columnList = "merchant_code"),
    @Index(name = "idx_merchant_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "merchant_code", unique = true, nullable = false, length = 50)
    private String merchantCode;
    
    @Column(name = "business_name", nullable = false, length = 255)
    private String businessName;
    
    @Column(unique = true, nullable = false, length = 255)
    private String email;
    
    @Column(name = "api_key_hash", nullable = false, length = 255)
    private String apiKeyHash;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MerchantStatus status;
    
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;
    
    @Column(name = "webhook_secret", length = 255)
    private String webhookSecret;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

