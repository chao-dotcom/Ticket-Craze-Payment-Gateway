package com.yourname.paymentgateway.dto.request;

import com.yourname.paymentgateway.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTransactionRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Invalid amount format")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be uppercase letters")
    private String currency = "USD";
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    @Size(max = 500, message = "Description too long")
    private String description;
    
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email too long")
    private String customerEmail;
    
    @Size(max = 255, message = "Customer name too long")
    private String customerName;
}

