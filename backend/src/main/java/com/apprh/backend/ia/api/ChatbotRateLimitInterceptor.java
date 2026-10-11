package com.apprh.backend.ia.api;

import com.apprh.backend.common.exception.ApiException;
import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.ia.infrastructure.ChatbotMetrics;
import com.apprh.backend.ia.infrastructure.ChatbotRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ChatbotRateLimitInterceptor implements HandlerInterceptor {

    private final ChatbotRateLimiter rateLimiter;
    private final ChatbotMetrics metrics;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimiter.tryAcquire(resolveKey(request))) {
            metrics.recordRateLimited();
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "CHATBOT_RATE_LIMITED",
                    "Trop de requêtes au chatbot. Veuillez patienter quelques instants.");
        }
        return true;
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return "user:" + principal.id();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
