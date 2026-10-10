package com.apprh.backend.ia.infrastructure;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limiteur de débit en mémoire (fenêtre glissante) pour les appels au chatbot.
 * Adapté à un déploiement mono-instance ; une clé est suivie par utilisateur.
 */
@Component
public class ChatbotRateLimiter {

    private static final int CLEANUP_THRESHOLD = 1000;

    private final int maxRequests;
    private final long windowMs;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public ChatbotRateLimiter(ChatbotProperties properties) {
        this.maxRequests = Math.max(1, properties.rateLimitMax());
        this.windowMs = Math.max(1_000L, properties.rateLimitWindowSeconds() * 1000L);
    }

    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMs) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
        }
        cleanupIfNeeded(now);
        return true;
    }

    private void cleanupIfNeeded(long now) {
        if (hits.size() < CLEANUP_THRESHOLD) {
            return;
        }
        Iterator<Map.Entry<String, Deque<Long>>> iterator = hits.entrySet().iterator();
        while (iterator.hasNext()) {
            Deque<Long> timestamps = iterator.next().getValue();
            synchronized (timestamps) {
                if (timestamps.isEmpty() || now - timestamps.peekFirst() >= windowMs) {
                    iterator.remove();
                }
            }
        }
    }
}
