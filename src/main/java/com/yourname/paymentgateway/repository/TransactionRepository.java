package com.yourname.paymentgateway.repository;

import com.yourname.paymentgateway.entity.Merchant;
import com.yourname.paymentgateway.entity.Transaction;
import com.yourname.paymentgateway.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(UUID transactionId);
    Optional<Transaction> findByMerchantAndTransactionId(Merchant merchant, UUID transactionId);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    @Query("SELECT t FROM Transaction t WHERE t.merchant = :merchant " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:startDate IS NULL OR t.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR t.createdAt <= :endDate) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findByMerchantAndFilters(
        @Param("merchant") Merchant merchant,
        @Param("status") TransactionStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}

