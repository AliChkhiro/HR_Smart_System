package com.apprh.backend.ia.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.chatbot")
public record ChatbotProperties(
        String baseUrl,
        String model,
        long timeoutMs,
        int rateLimitMax,
        long rateLimitWindowSeconds
) {
}
