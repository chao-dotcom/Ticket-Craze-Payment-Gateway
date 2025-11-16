package com.yourname.paymentgateway.dto.response;

import com.yourname.paymentgateway.enums.RefundStatus;
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
public class RefundResponse {
    private UUID refundId;
    private UUID transactionId;
    private BigDecimal amount;
    private String reason;
    private RefundStatus status;
    private String initiatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}

