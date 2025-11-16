package com.yourname.paymentgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourname.paymentgateway.entity.Merchant;
import com.yourname.paymentgateway.entity.Refund;
import com.yourname.paymentgateway.entity.Transaction;
import com.yourname.paymentgateway.entity.WebhookEvent;
import com.yourname.paymentgateway.repository.MerchantRepository;
import com.yourname.paymentgateway.repository.WebhookEventRepository;
import com.yourname.paymentgateway.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {
    
    private final WebhookEventRepository webhookEventRepository;
    private final MerchantRepository merchantRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    @Value("${payment.webhook.retry.max-attempts:5}")
    private int maxAttempts;
    
    @Value("${payment.webhook.retry.initial-delay:1000}")
    private long initialDelay;
    
    @Value("${payment.webhook.retry.multiplier:2}")
    private long multiplier;
    
    @Async
    @Transactional
    public void sendTransactionCompletedEvent(Transaction transaction) {
        Merchant merchant = transaction.getMerchant();
        if (merchant.getWebhookUrl() == null) {
            log.debug("No webhook URL configured for merchant: {}", merchant.getId());
            return;
        }
        
        createAndSendWebhook(
            merchant,
            transaction.getId(),
            WebhookEvent.WebhookEventType.TRANSACTION_COMPLETED,
            buildTransactionPayload(transaction)
        );
    }
    
    @Async
    @Transactional
    public void sendTransactionFailedEvent(Transaction transaction) {
        Merchant merchant = transaction.getMerchant();
        if (merchant.getWebhookUrl() == null) {
            return;
        }
        
        createAndSendWebhook(
            merchant,
            transaction.getId(),
            WebhookEvent.WebhookEventType.TRANSACTION_FAILED,
            buildTransactionPayload(transaction)
        );
    }
    
    @Async
    @Transactional
    public void sendRefundCompletedEvent(Refund refund) {
        Transaction transaction = refund.getTransaction();
        Merchant merchant = transaction.getMerchant();
        if (merchant.getWebhookUrl() == null) {
            return;
        }
        
        createAndSendWebhook(
            merchant,
            transaction.getId(),
            WebhookEvent.WebhookEventType.REFUND_COMPLETED,
            buildRefundPayload(refund)
        );
    }
    
    @SneakyThrows
    private void createAndSendWebhook(
        Merchant merchant,
        Long transactionId,
        WebhookEvent.WebhookEventType eventType,
        Map<String, Object> payload
    ) {
        String payloadJson = objectMapper.writeValueAsString(payload);
        
        WebhookEvent webhookEvent = WebhookEvent.builder()
            .merchantId(merchant.getId())
            .transactionId(transactionId)
            .eventType(eventType)
            .payload(payloadJson)
            .status(WebhookEvent.WebhookStatus.PENDING)
            .maxAttempts(maxAttempts)
            .nextRetryAt(LocalDateTime.now().plusSeconds(initialDelay / 1000))
            .build();
        
        webhookEvent = webhookEventRepository.save(webhookEvent);
        
        sendWebhook(webhookEvent);
    }
    
    @SneakyThrows
    private void sendWebhook(WebhookEvent webhookEvent) {
        Merchant merchant = merchantRepository.findById(webhookEvent.getMerchantId())
            .orElseThrow(() -> new RuntimeException("Merchant not found"));
        
        if (merchant.getWebhookUrl() == null) {
            webhookEvent.setStatus(WebhookEvent.WebhookStatus.FAILED);
            webhookEvent.setLastError("No webhook URL configured");
            webhookEventRepository.save(webhookEvent);
            return;
        }
        
        try {
            String payload = webhookEvent.getPayload();
            String signature = SignatureUtil.generateHmacSignature(
                payload,
                merchant.getWebhookSecret() != null ? merchant.getWebhookSecret() : "default-secret"
            );
            
            // In a real implementation, you would make HTTP POST request
            // For now, we'll just log it
            log.info("Sending webhook to {} for event type: {}", 
                merchant.getWebhookUrl(), webhookEvent.getEventType());
            
            // Simulate webhook call
            // restTemplate.postForObject(merchant.getWebhookUrl(), payload, String.class);
            
            webhookEvent.setStatus(WebhookEvent.WebhookStatus.SENT);
            webhookEvent.setSentAt(LocalDateTime.now());
            webhookEvent.setAttemptCount(webhookEvent.getAttemptCount() + 1);
            
        } catch (Exception e) {
            log.error("Failed to send webhook: {}", webhookEvent.getId(), e);
            webhookEvent.setAttemptCount(webhookEvent.getAttemptCount() + 1);
            webhookEvent.setLastError(e.getMessage());
            
            if (webhookEvent.getAttemptCount() >= webhookEvent.getMaxAttempts()) {
                webhookEvent.setStatus(WebhookEvent.WebhookStatus.FAILED);
            } else {
                long delay = (long) (initialDelay * Math.pow(multiplier, webhookEvent.getAttemptCount() - 1));
                webhookEvent.setNextRetryAt(LocalDateTime.now().plusSeconds(delay / 1000));
            }
        }
        
        webhookEventRepository.save(webhookEvent);
    }
    
    @Scheduled(fixedDelay = 60000) // Run every minute
    @Transactional
    public void retryFailedWebhooks() {
        List<WebhookEvent> pendingWebhooks = webhookEventRepository.findPendingWebhooksForRetry(
            WebhookEvent.WebhookStatus.PENDING,
            LocalDateTime.now()
        );
        
        for (WebhookEvent webhook : pendingWebhooks) {
            sendWebhook(webhook);
        }
    }
    
    private Map<String, Object> buildTransactionPayload(Transaction transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "transaction.completed");
        payload.put("transactionId", transaction.getTransactionId());
        payload.put("amount", transaction.getAmount());
        payload.put("currency", transaction.getCurrency());
        payload.put("status", transaction.getStatus());
        payload.put("timestamp", LocalDateTime.now());
        return payload;
    }
    
    private Map<String, Object> buildRefundPayload(Refund refund) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "refund.completed");
        payload.put("refundId", refund.getRefundId());
        payload.put("transactionId", refund.getTransaction().getTransactionId());
        payload.put("amount", refund.getAmount());
        payload.put("reason", refund.getReason());
        payload.put("timestamp", LocalDateTime.now());
        return payload;
    }
}

