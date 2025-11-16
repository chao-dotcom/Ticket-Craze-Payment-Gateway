package com.yourname.paymentgateway.integration;

import com.yourname.paymentgateway.dto.request.CreateTransactionRequest;
import com.yourname.paymentgateway.enums.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TransactionIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.5")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void createTransaction_WithValidRequest_ShouldReturn201() {
        // Arrange
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setAmount(new BigDecimal("150.00"));
        request.setCurrency("USD");
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setDescription("Integration test payment");
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "test-api-key");  // Configure in test data
        headers.set("Idempotency-Key", "test-idem-" + System.currentTimeMillis());
        
        HttpEntity<CreateTransactionRequest> entity = new HttpEntity<>(request, headers);
        
        // Act
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/transactions",
            HttpMethod.POST,
            entity,
            String.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("transactionId");
    }
    
    @Test
    void healthCheck_ShouldReturn200() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/health",
            String.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("status");
    }
}

