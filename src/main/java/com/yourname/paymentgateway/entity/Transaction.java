package com.yourname.paymentgateway.entity;

import com.yourname.paymentgateway.enums.PaymentMethod;
import com.yourname.paymentgateway.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_merchant_created", columnList = "merchant_id, created_at"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_transaction_id", columnList = "transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id", nullable = false, unique = true, updatable = false)
    private UUID transactionId = UUID.randomUUID();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;
    
    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "customer_email")
    private String customerEmail;
    
    @Column(name = "customer_name")
    private String customerName;
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "failed_at")
    private LocalDateTime failedAt;
    
    public void transitionTo(TransactionStatus newStatus) {
        if (!isValidTransition(this.status, newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid transition from %s to %s", this.status, newStatus)
            );
        }
        this.status = newStatus;
        
        if (newStatus == TransactionStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        } else if (newStatus == TransactionStatus.FAILED) {
            this.failedAt = LocalDateTime.now();
        }
    }
    
    private boolean isValidTransition(TransactionStatus from, TransactionStatus to) {
        return switch (from) {
            case PENDING -> to == TransactionStatus.PROCESSING || to == TransactionStatus.FAILED;
            case PROCESSING -> to == TransactionStatus.COMPLETED || to == TransactionStatus.FAILED;
            case COMPLETED -> to == TransactionStatus.REFUNDED || to == TransactionStatus.PARTIALLY_REFUNDED;
            default -> false;
        };
    }
    
    @PrePersist
    protected void onCreate() {
        if (transactionId == null) {
            transactionId = UUID.randomUUID();
        }
    }
}

