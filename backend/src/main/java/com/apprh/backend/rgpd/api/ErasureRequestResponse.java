package com.apprh.backend.rgpd.api;

import com.apprh.backend.rgpd.domain.ErasureStatus;

import java.time.Instant;

public record ErasureRequestResponse(
        Long id,
        Long userId,
        String userEmail,
        ErasureStatus status,
        String reason,
        Instant requestedAt,
        Instant decidedAt,
        Long decidedByUserId
) {
}
