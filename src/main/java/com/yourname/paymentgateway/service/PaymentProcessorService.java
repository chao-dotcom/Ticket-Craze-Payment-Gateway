package com.yourname.paymentgateway.service;

import com.yourname.paymentgateway.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class PaymentProcessorService {
    
    @Value("${app.payment-processor.success-rate:0.9}")
    private double successRate;
    
    private final Random random = new Random();
    
    /**
     * Simulates external payment processor API call.
     * Configurable success rate (default 90%).
     */
    public PaymentResult process(Transaction transaction) {
        log.info("Processing payment for transaction: {}", transaction.getTransactionId());
        
        // Simulate processing delay
        try {
            Thread.sleep(1000 + random.nextInt(2000)); // 1-3 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate success based on configured rate
        boolean success = random.nextDouble() < successRate;
        
        if (success) {
            return new PaymentResult(
                true,
                "APPROVED",
                "Payment processed successfully",
                generateProcessorTransactionId()
            );
        } else {
            return new PaymentResult(
                false,
                "DECLINED",
                simulateErrorMessage(),
                null
            );
        }
    }
    
    private String generateProcessorTransactionId() {
        return "PROC_" + System.currentTimeMillis() + "_" + random.nextInt(10000);
    }
    
    private String simulateErrorMessage() {
        String[] errors = {
            "Insufficient funds",
            "Card declined by issuer",
            "Invalid card number",
            "Expired card",
            "Transaction limit exceeded"
        };
        return errors[random.nextInt(errors.length)];
    }
    
    @Data
    @AllArgsConstructor
    public static class PaymentResult {
        private boolean successful;
        private String statusCode;
        private String message;
        private String processorTransactionId;
        
        public String getErrorMessage() {
            return successful ? null : message;
        }
    }
}

