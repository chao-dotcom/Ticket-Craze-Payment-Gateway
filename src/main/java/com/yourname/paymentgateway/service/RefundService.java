package com.yourname.paymentgateway.service;

import com.yourname.paymentgateway.dto.request.RefundRequest;
import com.yourname.paymentgateway.dto.response.RefundResponse;
import com.yourname.paymentgateway.entity.Merchant;
import com.yourname.paymentgateway.entity.Refund;
import com.yourname.paymentgateway.entity.Transaction;
import com.yourname.paymentgateway.enums.RefundStatus;
import com.yourname.paymentgateway.exception.TransactionNotFoundException;
import com.yourname.paymentgateway.repository.RefundRepository;
import com.yourname.paymentgateway.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {
    
    private final RefundRepository refundRepository;
    private final TransactionRepository transactionRepository;
    private final WebhookService webhookService;
    
    @Transactional
    public RefundResponse createRefund(
        Merchant merchant,
        UUID transactionId,
        RefundRequest request
    ) {
        Transaction transaction = transactionRepository
            .findByMerchantAndTransactionId(merchant, transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        
        // Validate transaction can be refunded
        if (transaction.getStatus() != com.yourname.paymentgateway.enums.TransactionStatus.COMPLETED) {
            throw new IllegalStateException(
                "Transaction must be COMPLETED to process refund. Current status: " + transaction.getStatus()
            );
        }
        
        // Validate refund amount doesn't exceed transaction amount
        if (request.getAmount().compareTo(transaction.getAmount()) > 0) {
            throw new IllegalArgumentException(
                "Refund amount cannot exceed transaction amount"
            );
        }
        
        // Check existing refunds
        BigDecimal totalRefunded = refundRepository.findByTransactionId(transaction.getId())
            .stream()
            .filter(r -> r.getStatus() == RefundStatus.COMPLETED)
            .map(Refund::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal remainingAmount = transaction.getAmount().subtract(totalRefunded);
        if (request.getAmount().compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException(
                String.format("Refund amount exceeds remaining refundable amount. Remaining: %s", remainingAmount)
            );
        }
        
        // Create refund
        Refund refund = Refund.builder()
            .transaction(transaction)
            .amount(request.getAmount())
            .reason(request.getReason())
            .status(RefundStatus.PENDING)
            .initiatedBy(merchant.getMerchantCode())
            .build();
        
        refund = refundRepository.save(refund);
        
        // Process refund asynchronously
        processRefundAsync(refund.getId());
        
        return mapToResponse(refund);
    }
    
    @org.springframework.scheduling.annotation.Async
    public void processRefundAsync(Long refundId) {
        processRefund(refundId);
    }
    
    @Transactional
    public void processRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
            .orElseThrow(() -> new RuntimeException("Refund not found"));
        
        try {
            refund.setStatus(RefundStatus.PROCESSING);
            refundRepository.save(refund);
            
            // Simulate refund processing
            Thread.sleep(500);
            
            refund.setStatus(RefundStatus.COMPLETED);
            refund.setProcessedAt(java.time.LocalDateTime.now());
            refundRepository.save(refund);
            
            // Update transaction status if fully refunded
            Transaction transaction = refund.getTransaction();
            BigDecimal totalRefunded = refundRepository.findByTransactionId(transaction.getId())
                .stream()
                .filter(r -> r.getStatus() == RefundStatus.COMPLETED)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalRefunded.compareTo(transaction.getAmount()) == 0) {
                transaction.transitionTo(com.yourname.paymentgateway.enums.TransactionStatus.REFUNDED);
            } else {
                transaction.transitionTo(com.yourname.paymentgateway.enums.TransactionStatus.PARTIALLY_REFUNDED);
            }
            transactionRepository.save(transaction);
            
            // Send webhook notification
            webhookService.sendRefundCompletedEvent(refund);
            
        } catch (Exception e) {
            log.error("Refund processing failed for refund: {}", refundId, e);
            refund.setStatus(RefundStatus.FAILED);
            refundRepository.save(refund);
        }
    }
    
    @Transactional(readOnly = true)
    public RefundResponse getRefund(Merchant merchant, UUID refundId) {
        Refund refund = refundRepository.findByRefundId(refundId)
            .orElseThrow(() -> new RuntimeException("Refund not found"));
        
        // Verify merchant owns the transaction
        if (!refund.getTransaction().getMerchant().getId().equals(merchant.getId())) {
            throw new RuntimeException("Refund not found");
        }
        
        return mapToResponse(refund);
    }
    
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByTransaction(Merchant merchant, UUID transactionId) {
        Transaction transaction = transactionRepository
            .findByMerchantAndTransactionId(merchant, transactionId)
            .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        
        return refundRepository.findByTransactionId(transaction.getId())
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    private RefundResponse mapToResponse(Refund refund) {
        return RefundResponse.builder()
            .refundId(refund.getRefundId())
            .transactionId(refund.getTransaction().getTransactionId())
            .amount(refund.getAmount())
            .reason(refund.getReason())
            .status(refund.getStatus())
            .initiatedBy(refund.getInitiatedBy())
            .createdAt(refund.getCreatedAt())
            .processedAt(refund.getProcessedAt())
            .build();
    }
}

