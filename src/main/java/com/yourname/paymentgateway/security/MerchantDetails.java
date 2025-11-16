package com.yourname.paymentgateway.security;

import com.yourname.paymentgateway.entity.Merchant;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class MerchantDetails implements UserDetails {
    
    private final Merchant merchant;
    
    public MerchantDetails(Merchant merchant) {
        this.merchant = merchant;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_MERCHANT"));
    }
    
    @Override
    public String getPassword() {
        return null;
    }
    
    @Override
    public String getUsername() {
        return merchant.getMerchantCode();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return merchant.getStatus().name().equals("ACTIVE");
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return merchant.getStatus().name().equals("ACTIVE");
    }
}

