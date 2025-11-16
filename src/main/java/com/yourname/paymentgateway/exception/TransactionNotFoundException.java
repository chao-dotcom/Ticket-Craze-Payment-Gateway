package com.yourname.paymentgateway.exception;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {
    
    public TransactionNotFoundException(UUID transactionId) {
        super("Transaction not found: " + transactionId);
    }
    
    public TransactionNotFoundException(Long transactionId) {
        super("Transaction not found with ID: " + transactionId);
    }
}

