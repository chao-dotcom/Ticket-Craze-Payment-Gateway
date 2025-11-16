package com.yourname.paymentgateway.repository;

import com.yourname.paymentgateway.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByRefundId(UUID refundId);
    List<Refund> findByTransactionId(Long transactionId);
}

