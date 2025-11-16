package com.yourname.paymentgateway.controller;

import com.yourname.paymentgateway.dto.request.RefundRequest;
import com.yourname.paymentgateway.dto.response.RefundResponse;
import com.yourname.paymentgateway.security.MerchantDetails;
import com.yourname.paymentgateway.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions/{transactionId}/refunds")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "Refund management APIs")
@SecurityRequirement(name = "API-Key")
public class RefundController {
    
    private final RefundService refundService;
    
    @PostMapping
    @Operation(summary = "Create a refund for a transaction")
    public ResponseEntity<RefundResponse> createRefund(
        @AuthenticationPrincipal MerchantDetails merchantDetails,
        @PathVariable UUID transactionId,
        @Valid @RequestBody RefundRequest request
    ) {
        RefundResponse response = refundService.createRefund(
            merchantDetails.getMerchant(),
            transactionId,
            request
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all refunds for a transaction")
    public ResponseEntity<List<RefundResponse>> getRefunds(
        @AuthenticationPrincipal MerchantDetails merchantDetails,
        @PathVariable UUID transactionId
    ) {
        List<RefundResponse> refunds = refundService.getRefundsByTransaction(
            merchantDetails.getMerchant(),
            transactionId
        );
        
        return ResponseEntity.ok(refunds);
    }
    
    @GetMapping("/{refundId}")
    @Operation(summary = "Get refund by ID")
    public ResponseEntity<RefundResponse> getRefund(
        @AuthenticationPrincipal MerchantDetails merchantDetails,
        @PathVariable UUID transactionId,
        @PathVariable UUID refundId
    ) {
        RefundResponse response = refundService.getRefund(
            merchantDetails.getMerchant(),
            refundId
        );
        
        return ResponseEntity.ok(response);
    }
}

