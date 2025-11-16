package com.yourname.paymentgateway.repository;

import com.yourname.paymentgateway.entity.ApiRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ApiRateLimitRepository extends JpaRepository<ApiRateLimit, Long> {
    
    Optional<ApiRateLimit> findByMerchantIdAndWindowStart(Long merchantId, LocalDateTime windowStart);
    
    @Modifying
    @Query("UPDATE ApiRateLimit a SET a.requestCount = a.requestCount + 1 " +
           "WHERE a.merchantId = :merchantId AND a.windowStart = :windowStart")
    void incrementRequestCount(@Param("merchantId") Long merchantId, @Param("windowStart") LocalDateTime windowStart);
    
    @Modifying
    @Query("DELETE FROM ApiRateLimit a WHERE a.windowStart < :cutoff")
    void deleteOldWindows(@Param("cutoff") LocalDateTime cutoff);
}

