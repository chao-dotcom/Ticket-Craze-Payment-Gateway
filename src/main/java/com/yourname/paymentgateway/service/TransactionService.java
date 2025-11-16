package com.yourname.paymentgateway.service;

import com.yourname.paymentgateway.dto.request.CreateTransactionRequest;
import com.yourname.paymentgateway.dto.response.TransactionResponse;
import com.yourname.paymentgateway.entity.Merchant;
import com.yourname.paymentgateway.entity.Transaction;
import com.yourname.paymentgateway.entity.TransactionHistory;
import com.yourname.paymentgateway.enums.TransactionStatus;
import com.yourname.paymentgateway.exception.PaymentProcessorException;
import com.yourname.paymentgateway.exception.TransactionNotFoundException;
import com.yourname.paymentgateway.repository.TransactionHistoryRepository;
import com.yourname.paymentgateway.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final TransactionHistoryRepository historyRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentProcessorService paymentProcessor;
    private final WebhookService webhookService;
    
    @Transactional
    public TransactionResponse createTransaction(
        Merchant merchant,
        String idempotencyKey,
        CreateTransactionRequest request
    ) {
        log.info("Creating transaction for merchant: {}, idempotency key: {}", 
                 merchant.getId(), idempotencyKey);
        
        // Check for duplicate request
        var cachedResponse = idempotencyService.getCachedResponse(
            merchant.getId(), 
            idempotencyKey
        );
        if (cachedResponse != null) {
            log.info("Returning cached response for idempotency key: {}", idempotencyKey);
            return cachedResponse;
        }
        
        // Create new transaction
        Transaction transaction = Transaction.builder()
            .merchant(merchant)
            .idempotencyKey(idempotencyKey)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .paymentMethod(request.getPaymentMethod())
            .description(request.getDescription())
            .customerEmail(request.getCustomerEmail())
            .customerName(request.getCustomerName())
            .status(TransactionStatus.PENDING)
            .build();
        
        transaction = transactionRepository.save(transaction);
        
        // Log initial state
        logStateChange(transaction, null, TransactionStatus.PENDING, "Transaction created");
        
        // Asynchronously process payment
        processPaymentAsync(transaction.getId());
        
        TransactionResponse response = mapToResponse(transaction);
        
        // Cache response for idempotency
        idempotencyService.cacheResponse(
            merchant.getId(),
            idempotencyKey,
            response
        );
        
        return response;
    }
    
    @Async
    public void processPaymentAsync(Long transactionId) {
        processPayment(transactionId);
    }
    
    @Transactional
    @Retryable(
        value = {PaymentProcessorException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void processPayment(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        
        try {
            // Transition to PROCESSING
            transaction.transitionTo(TransactionStatus.PROCESSING);
            transactionRepository.save(transaction);
            logStateChange(transaction, TransactionStatus.PENDING, 
                          TransactionStatus.PROCESSING, "Payment processing started");
            
            // Call external payment processor (mock)
            PaymentProcessorService.PaymentResult result = paymentProcessor.process(transaction);
            
            if (result.isSuccessful()) {
                transaction.transitionTo(TransactionStatus.COMPLETED);
                logStateChange(transaction, TransactionStatus.PROCESSING, 
                              TransactionStatus.COMPLETED, "Payment successful");
                
                // Send webhook notification
                webhookService.sendTransactionCompletedEvent(transaction);
            } else {
                transaction.transitionTo(TransactionStatus.FAILED);
                logStateChange(transaction, TransactionStatus.PROCESSING, 
                              TransactionStatus.FAILED, 
                              "Payment failed: " + result.getErrorMessage());
                
                // Send webhook notification for failure
                webhookService.sendTransactionFailedEvent(transaction);
            }
            
            transactionRepository.save(transaction);
            
        } catch (Exception e) {
            log.error("Payment processing failed for transaction: {}", transactionId, e);
            transaction.transitionTo(TransactionStatus.FAILED);
            logStateChange(transaction, transaction.getStatus(), 
                          TransactionStatus.FAILED, 
                          "Exception: " + e.getMessage());
            transactionRepository.save(transaction);
            throw new PaymentProcessorException("Payment processing failed", e);
        }
    }
    
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Merchant merchant, UUID transactionId) {
        Transaction transaction = transactionRepository
            .findByMerchantAndTransactionId(merchant, transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        
        return mapToResponse(transaction);
    }
    
    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(
        Merchant merchant,
        String status,
        String startDate,
        String endDate,
        Pageable pageable
    ) {
        TransactionStatus statusEnum = status != null ? TransactionStatus.valueOf(status) : null;
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;
        
        return transactionRepository.findByMerchantAndFilters(
            merchant, statusEnum, start, end, pageable
        ).map(this::mapToResponse);
    }
    
    @Transactional(readOnly = true)
    public List<TransactionHistory> getTransactionHistory(Merchant merchant, UUID transactionId) {
        Transaction transaction = transactionRepository
            .findByMerchantAndTransactionId(merchant, transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        
        return historyRepository.findByTransactionOrderByChangedAtAsc(transaction);
    }
    
    private void logStateChange(Transaction transaction, 
                                TransactionStatus fromStatus,
                                TransactionStatus toStatus, 
                                String reason) {
        TransactionHistory history = TransactionHistory.builder()
            .transaction(transaction)
            .fromStatus(fromStatus)
            .toStatus(toStatus)
            .reason(reason)
            .changedBy("SYSTEM")
            .build();
        
        historyRepository.save(history);
    }
    
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
            .transactionId(transaction.getTransactionId())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .status(transaction.getStatus())
            .paymentMethod(transaction.getPaymentMethod())
            .description(transaction.getDescription())
            .customerEmail(transaction.getCustomerEmail())
            .customerName(transaction.getCustomerName())
            .createdAt(transaction.getCreatedAt())
            .completedAt(transaction.getCompletedAt())
            .failedAt(transaction.getFailedAt())
            .build();
    }
    
    // Note: TransactionMapper can be used instead for better separation of concerns
}

