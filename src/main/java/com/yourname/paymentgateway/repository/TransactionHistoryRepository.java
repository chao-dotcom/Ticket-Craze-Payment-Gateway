package com.yourname.paymentgateway.repository;

import com.yourname.paymentgateway.entity.Transaction;
import com.yourname.paymentgateway.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
    List<TransactionHistory> findByTransactionOrderByChangedAtAsc(Transaction transaction);
}

