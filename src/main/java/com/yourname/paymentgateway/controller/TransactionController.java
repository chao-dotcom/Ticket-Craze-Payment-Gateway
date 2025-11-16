package com.yourname.paymentgateway.controller;

import com.yourname.paymentgateway.dto.request.CreateTransactionRequest;
import com.yourname.paymentgateway.dto.response.TransactionResponse;
import com.yourname.paymentgateway.entity.Merchant;
import com.yourname.paymentgateway.entity.TransactionHistory;
import com.yourname.paymentgateway.security.MerchantDetails;
import com.yourname.paymentgateway.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management APIs")
@SecurityRequirement(name = "API-Key")
public class TransactionController {
    
    private final TransactionService transactionService;
    
    /**
     * POST /api/v1/transactions
     * Creates a new transaction with idempotency support.
     */
    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<TransactionResponse> createTransaction(
        @AuthenticationPrincipal MerchantDetails merchantDetails,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CreateTransactionRequest request
    ) {
        Merchant merchant = merchantDetails.getMerchant();
        
        TransactionResponse response = transactionService.createTransaction(
            merchant,
            idempotencyKey,
            request
        );
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
    
    /**
     * GET /api/v1/transactions/{transactionId}
     * Retrieves a specific transaction by ID.
     */
    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransactionResponse> getTransaction(
        @AuthenticationPrincipal MerchantDetails merchantDetails,
        @PathVariable UUID transactionId
    ) {
        TransactionResponse response = transactionService.getTransaction(
            merchantDetails.getMerchant(),
            transactionId
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/transactions
     * Lists transactions with pagination and filtering.
     */
    @GetMapping
    @Operation(summary = "List transactions with pagination")
    public ResponseEntity<Page<TransactionResponse>> listTransactions(
        @AuthenticationPrincipal MerchantDetails merchantDetails,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        Pageable pageable
    ) {
        Page<TransactionResponse> transactions = transactionService.listTransactions(
            merchantDetails.getMerchant(),
            status,
            startDate,
            endDate,
            pageable
        );
        
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * GET /api/v1/transactions/{transactionId}/history
     * Retrieves audit trail for a transaction.
     */
    @GetMapping("/{transactionId}/history")
    @Operation(summary = "Get transaction history")
    public ResponseEntity<?> getTransactionHistory(
        @AuthenticationPrincipal MerchantDetails merchantDetails,
        @PathVariable UUID transactionId
    ) {
        List<TransactionHistory> history = transactionService.getTransactionHistory(
            merchantDetails.getMerchant(),
            transactionId
        );
        
        return ResponseEntity.ok(history);
    }
}

