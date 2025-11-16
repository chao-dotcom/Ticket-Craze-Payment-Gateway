package com.yourname.paymentgateway.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySummaryResponse {
    private LocalDate date;
    private Long totalTransactions;
    private Long completedTransactions;
    private Long failedTransactions;
    private BigDecimal totalAmount;
}

