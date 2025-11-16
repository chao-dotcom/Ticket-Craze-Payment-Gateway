package com.yourname.paymentgateway.repository;

import com.yourname.paymentgateway.entity.IdempotencyCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyCacheRepository extends JpaRepository<IdempotencyCache, Long> {
    Optional<IdempotencyCache> findByMerchantIdAndIdempotencyKey(Long merchantId, String idempotencyKey);
    
    @Modifying
    @Query("DELETE FROM IdempotencyCache c WHERE c.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}

