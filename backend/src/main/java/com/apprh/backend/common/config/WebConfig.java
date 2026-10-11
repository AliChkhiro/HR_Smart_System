package com.apprh.backend.common.config;

import com.apprh.backend.ia.api.ChatbotRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ChatbotRateLimitInterceptor chatbotRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Seuls les appels coûteux (LLM) sont limités ; la lecture/le nettoyage de
        // l'historique (GET/DELETE /chatbot/messages) ne consomme pas le quota.
        registry.addInterceptor(chatbotRateLimitInterceptor)
                .addPathPatterns("/chatbot/chat", "/chatbot/chat/stream");
    }
}
