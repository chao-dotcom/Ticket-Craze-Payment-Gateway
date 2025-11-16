package com.yourname.paymentgateway.repository;

import com.yourname.paymentgateway.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    
    List<WebhookEvent> findByMerchantIdAndStatusOrderByCreatedAtDesc(
        Long merchantId, 
        WebhookEvent.WebhookStatus status
    );
    
    @Query("SELECT w FROM WebhookEvent w WHERE w.status = :status " +
           "AND (w.nextRetryAt IS NULL OR w.nextRetryAt <= :now) " +
           "AND w.attemptCount < w.maxAttempts " +
           "ORDER BY w.createdAt ASC")
    List<WebhookEvent> findPendingWebhooksForRetry(
        @Param("status") WebhookEvent.WebhookStatus status,
        @Param("now") LocalDateTime now
    );
}

