package com.yourname.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourname.paymentgateway.dto.request.CreateTransactionRequest;
import com.yourname.paymentgateway.dto.response.TransactionResponse;
import com.yourname.paymentgateway.entity.Merchant;
import com.yourname.paymentgateway.enums.MerchantStatus;
import com.yourname.paymentgateway.enums.PaymentMethod;
import com.yourname.paymentgateway.enums.TransactionStatus;
import com.yourname.paymentgateway.security.MerchantDetails;
import com.yourname.paymentgateway.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TransactionService transactionService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private Merchant merchant;
    private MerchantDetails merchantDetails;
    private CreateTransactionRequest request;
    
    @BeforeEach
    void setUp() {
        merchant = Merchant.builder()
            .id(1L)
            .merchantCode("TEST_MERCHANT")
            .businessName("Test Business")
            .email("test@example.com")
            .status(MerchantStatus.ACTIVE)
            .build();
        
        merchantDetails = new MerchantDetails(merchant);
        
        request = new CreateTransactionRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setDescription("Test payment");
    }
    
    @Test
    void createTransaction_WithValidRequest_ShouldReturn201() throws Exception {
        // Arrange
        String idempotencyKey = "test-key-123";
        TransactionResponse response = TransactionResponse.builder()
            .transactionId(UUID.randomUUID())
            .amount(request.getAmount())
            .status(TransactionStatus.PENDING)
            .build();
        
        when(transactionService.createTransaction(any(), eq(idempotencyKey), any()))
            .thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions")
                .with(user(merchantDetails))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transactionId").exists())
            .andExpect(jsonPath("$.amount").value(100.00))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }
    
    @Test
    void getTransaction_WithValidId_ShouldReturn200() throws Exception {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        TransactionResponse response = TransactionResponse.builder()
            .transactionId(transactionId)
            .amount(new BigDecimal("100.00"))
            .status(TransactionStatus.COMPLETED)
            .build();
        
        when(transactionService.getTransaction(any(), eq(transactionId)))
            .thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/transactions/{transactionId}", transactionId)
                .with(user(merchantDetails)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}

