package com.yourname.paymentgateway.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SignatureUtil {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    /**
     * Generates HMAC-SHA256 signature for webhook payload.
     * 
     * @param payload The JSON payload to sign
     * @param secret The secret key for HMAC
     * @return Base64 encoded signature
     */
    public static String generateHmacSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }
    
    /**
     * Verifies HMAC signature.
     * 
     * @param payload The JSON payload
     * @param signature The signature to verify
     * @param secret The secret key
     * @return true if signature is valid
     */
    public static boolean verifySignature(String payload, String signature, String secret) {
        String expectedSignature = generateHmacSignature(payload, secret);
        return expectedSignature.equals(signature);
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

