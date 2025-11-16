package com.yourname.paymentgateway.repository;

import com.yourname.paymentgateway.entity.Merchant;
import com.yourname.paymentgateway.entity.Transaction;
import com.yourname.paymentgateway.enums.MerchantStatus;
import com.yourname.paymentgateway.enums.PaymentMethod;
import com.yourname.paymentgateway.enums.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TransactionRepositoryTest {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private MerchantRepository merchantRepository;
    
    private Merchant merchant;
    private Transaction transaction;
    
    @BeforeEach
    void setUp() {
        merchant = Merchant.builder()
            .merchantCode("TEST_MERCHANT")
            .businessName("Test Business")
            .email("test@example.com")
            .apiKeyHash("test-hash")
            .status(MerchantStatus.ACTIVE)
            .build();
        merchant = merchantRepository.save(merchant);
        
        transaction = Transaction.builder()
            .merchant(merchant)
            .idempotencyKey("test-key-123")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .status(TransactionStatus.PENDING)
            .paymentMethod(PaymentMethod.CREDIT_CARD)
            .build();
    }
    
    @Test
    void save_ShouldPersistTransaction() {
        // Act
        Transaction saved = transactionRepository.save(transaction);
        
        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTransactionId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }
    
    @Test
    void findByTransactionId_WithValidId_ShouldReturnTransaction() {
        // Arrange
        Transaction saved = transactionRepository.save(transaction);
        UUID transactionId = saved.getTransactionId();
        
        // Act
        Optional<Transaction> found = transactionRepository.findByTransactionId(transactionId);
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getTransactionId()).isEqualTo(transactionId);
    }
    
    @Test
    void findByIdempotencyKey_WithValidKey_ShouldReturnTransaction() {
        // Arrange
        transactionRepository.save(transaction);
        
        // Act
        Optional<Transaction> found = transactionRepository.findByIdempotencyKey("test-key-123");
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getIdempotencyKey()).isEqualTo("test-key-123");
    }
}

