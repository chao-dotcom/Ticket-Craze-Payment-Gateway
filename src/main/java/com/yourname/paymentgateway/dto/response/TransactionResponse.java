package com.yourname.paymentgateway.dto.response;

import com.yourname.paymentgateway.enums.PaymentMethod;
import com.yourname.paymentgateway.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID transactionId;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private PaymentMethod paymentMethod;
    private String description;
    private String customerEmail;
    private String customerName;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
}

