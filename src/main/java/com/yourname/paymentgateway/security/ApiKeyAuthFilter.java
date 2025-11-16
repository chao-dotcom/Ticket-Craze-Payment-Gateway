package com.yourname.paymentgateway.security;

import com.yourname.paymentgateway.entity.Merchant;
import com.yourname.paymentgateway.repository.MerchantRepository;
import com.yourname.paymentgateway.util.HashUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    
    private static final String API_KEY_HEADER = "X-API-Key";
    private final MerchantRepository merchantRepository;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        String apiKey = request.getHeader(API_KEY_HEADER);
        
        if (apiKey != null) {
            try {
                authenticateApiKey(apiKey);
            } catch (BadCredentialsException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid API key\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private void authenticateApiKey(String apiKey) {
        String apiKeyHash = HashUtil.sha256(apiKey);
        
        Merchant merchant = merchantRepository
            .findByApiKeyHash(apiKeyHash)
            .orElseThrow(() -> new BadCredentialsException("Invalid API key"));
        
        if (!merchant.getStatus().name().equals("ACTIVE")) {
            throw new BadCredentialsException("Merchant account is not active");
        }
        
        MerchantDetails merchantDetails = new MerchantDetails(merchant);
        ApiKeyAuthenticationToken authentication = 
            new ApiKeyAuthenticationToken(merchantDetails);
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || 
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/");
    }
}

