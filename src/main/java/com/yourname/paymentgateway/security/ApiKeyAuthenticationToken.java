package com.yourname.paymentgateway.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
    
    private final MerchantDetails merchantDetails;
    
    public ApiKeyAuthenticationToken(MerchantDetails merchantDetails) {
        super(merchantDetails.getAuthorities());
        this.merchantDetails = merchantDetails;
        setAuthenticated(true);
    }
    
    @Override
    public Object getCredentials() {
        return null;
    }
    
    @Override
    public Object getPrincipal() {
        return merchantDetails;
    }
}

