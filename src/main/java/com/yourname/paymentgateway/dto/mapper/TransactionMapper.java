package com.yourname.paymentgateway.dto.mapper;

import com.yourname.paymentgateway.dto.response.TransactionResponse;
import com.yourname.paymentgateway.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    
    public TransactionResponse toResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        
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
}

