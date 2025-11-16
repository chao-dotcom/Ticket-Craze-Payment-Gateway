package com.yourname.paymentgateway.dto.request;

import com.yourname.paymentgateway.enums.TransactionStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TransactionFilterRequest {
    private TransactionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String customerEmail;
    private String paymentMethod;
}

