package com.yourname.paymentgateway.controller;

import com.yourname.paymentgateway.security.MerchantDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
@Tag(name = "Merchants", description = "Merchant management APIs")
@SecurityRequirement(name = "API-Key")
public class MerchantController {
    
    @GetMapping("/me")
    @Operation(summary = "Get current merchant information")
    public ResponseEntity<Map<String, Object>> getCurrentMerchant(
        @AuthenticationPrincipal MerchantDetails merchantDetails
    ) {
        var merchant = merchantDetails.getMerchant();
        
        Map<String, Object> response = new HashMap<>();
        response.put("merchantId", merchant.getId());
        response.put("merchantCode", merchant.getMerchantCode());
        response.put("businessName", merchant.getBusinessName());
        response.put("email", merchant.getEmail());
        response.put("status", merchant.getStatus());
        response.put("createdAt", merchant.getCreatedAt());
        
        return ResponseEntity.ok(response);
    }
}

