package com.apprh.backend.ia.infrastructure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Métriques Micrometer du chatbot, exposées via Actuator (/actuator/metrics).
 * - chatbot.requests : compteur de requêtes (tags surface, mode).
 * - chatbot.latency  : latence des appels au modèle (tag surface).
 * - chatbot.rate.limited : compteur de réponses 429.
 */
@Component
public class ChatbotMetrics {

    private final MeterRegistry registry;

    public ChatbotMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRequest(String surface, String mode) {
        registry.counter("chatbot.requests", "surface", surface, "mode", mode).increment();
    }

    public void recordLatency(String surface, long nanos) {
        Timer.builder("chatbot.latency")
                .tag("surface", surface)
                .register(registry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }

    public void recordRateLimited() {
        registry.counter("chatbot.rate.limited").increment();
    }
}
