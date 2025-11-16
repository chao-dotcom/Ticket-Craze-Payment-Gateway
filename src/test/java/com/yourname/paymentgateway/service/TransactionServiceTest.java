package com.yourname.paymentgateway.service;

import com.yourname.paymentgateway.dto.request.CreateTransactionRequest;
import com.yourname.paymentgateway.dto.response.TransactionResponse;
import com.yourname.paymentgateway.entity.Merchant;
import com.yourname.paymentgateway.entity.Transaction;
import com.yourname.paymentgateway.enums.PaymentMethod;
import com.yourname.paymentgateway.enums.TransactionStatus;
import com.yourname.paymentgateway.repository.TransactionHistoryRepository;
import com.yourname.paymentgateway.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private TransactionHistoryRepository historyRepository;
    
    @Mock
    private IdempotencyService idempotencyService;
    
    @Mock
    private PaymentProcessorService paymentProcessor;
    
    @Mock
    private WebhookService webhookService;
    
    @InjectMocks
    private TransactionService transactionService;
    
    private Merchant merchant;
    private CreateTransactionRequest request;
    
    @BeforeEach
    void setUp() {
        merchant = Merchant.builder()
            .id(1L)
            .merchantCode("TEST_MERCHANT")
            .businessName("Test Business")
            .email("test@example.com")
            .status(com.yourname.paymentgateway.enums.MerchantStatus.ACTIVE)
            .build();
        
        request = new CreateTransactionRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setDescription("Test payment");
    }
    
    @Test
    void createTransaction_WithNewIdempotencyKey_ShouldCreateTransaction() {
        // Arrange
        String idempotencyKey = "test-key-123";
        when(idempotencyService.getCachedResponse(anyLong(), anyString()))
            .thenReturn(null);
        
        Transaction savedTransaction = Transaction.builder()
            .id(1L)
            .transactionId(UUID.randomUUID())
            .merchant(merchant)
            .amount(request.getAmount())
            .status(TransactionStatus.PENDING)
            .build();
        
        when(transactionRepository.save(any(Transaction.class)))
            .thenReturn(savedTransaction);
        
        // Act
        var response = transactionService.createTransaction(
            merchant, 
            idempotencyKey, 
            request
        );
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualTo(request.getAmount());
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.PENDING);
        
        verify(transactionRepository).save(any(Transaction.class));
        verify(idempotencyService).cacheResponse(anyLong(), anyString(), any());
    }
    
    @Test
    void createTransaction_WithDuplicateIdempotencyKey_ShouldReturnCachedResponse() {
        // Arrange
        String idempotencyKey = "duplicate-key";
        var cachedResponse = TransactionResponse.builder()
            .transactionId(UUID.randomUUID())
            .amount(request.getAmount())
            .build();
        
        when(idempotencyService.getCachedResponse(anyLong(), anyString()))
            .thenReturn(cachedResponse);
        
        // Act
        var response = transactionService.createTransaction(
            merchant, 
            idempotencyKey, 
            request
        );
        
        // Assert
        assertThat(response).isEqualTo(cachedResponse);
        verify(transactionRepository, never()).save(any());
    }
    
    @Test
    void getTransaction_WithValidId_ShouldReturnTransaction() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
            .id(1L)
            .transactionId(transactionId)
            .merchant(merchant)
            .amount(new BigDecimal("100.00"))
            .status(TransactionStatus.COMPLETED)
            .build();
        
        when(transactionRepository.findByMerchantAndTransactionId(merchant, transactionId))
            .thenReturn(Optional.of(transaction));
        
        // Act
        var response = transactionService.getTransaction(merchant, transactionId);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(transactionId);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }
}

