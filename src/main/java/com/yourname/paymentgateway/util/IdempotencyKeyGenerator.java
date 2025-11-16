package com.yourname.paymentgateway.util;

import java.util.UUID;

public class IdempotencyKeyGenerator {
    
    /**
     * Generates a unique idempotency key.
     * In production, you might want to use a more sophisticated approach
     * that includes merchant ID and timestamp.
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generates an idempotency key with merchant context.
     */
    public static String generate(Long merchantId) {
        return String.format("%d-%s", merchantId, UUID.randomUUID().toString());
    }
    
    /**
     * Generates an idempotency key with merchant and timestamp.
     */
    public static String generate(Long merchantId, long timestamp) {
        return String.format("%d-%d-%s", merchantId, timestamp, UUID.randomUUID().toString());
    }
}

