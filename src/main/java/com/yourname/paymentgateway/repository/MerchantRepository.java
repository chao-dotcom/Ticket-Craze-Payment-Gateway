package com.yourname.paymentgateway.repository;

import com.yourname.paymentgateway.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByApiKeyHash(String apiKeyHash);
    Optional<Merchant> findByMerchantCode(String merchantCode);
    Optional<Merchant> findByEmail(String email);
}

