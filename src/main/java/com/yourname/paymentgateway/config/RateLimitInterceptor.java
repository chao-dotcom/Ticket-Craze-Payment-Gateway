package com.yourname.paymentgateway.config;

import com.yourname.paymentgateway.exception.RateLimitExceededException;
import com.yourname.paymentgateway.security.MerchantDetails;
import com.yourname.paymentgateway.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RateLimitService rateLimitService;
    
    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) throws Exception {
        
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof MerchantDetails) {
            MerchantDetails merchantDetails = (MerchantDetails) authentication.getPrincipal();
            Long merchantId = merchantDetails.getMerchant().getId();
            
            if (!rateLimitService.tryConsume(merchantId)) {
                response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
                response.setContentType("application/json");
                response.getWriter().write(
                    String.format(
                        "{\"error\": \"Rate limit exceeded\", \"retryAfter\": 60, \"availableTokens\": %d}",
                        rateLimitService.getAvailableTokens(merchantId)
                    )
                );
                return false;
            }
        }
        
        return true;
    }
}

