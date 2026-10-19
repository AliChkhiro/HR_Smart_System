package com.apprh.backend.rgpd.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Demande d'effacement de compte (droit a l'effacement, RGPD article 17).
 *
 * L'effacement n'est jamais automatique : l'utilisateur depose une demande,
 * un administrateur la valide (suppression douce du compte) ou la refuse.
 */
@Entity
@Table(name = "erasure_request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErasureRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ErasureStatus status = ErasureStatus.PENDING;

    @Column(length = 500)
    private String reason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by_user_id")
    private Long decidedByUserId;

    @PrePersist
    void onCreate() {
        requestedAt = Instant.now();
    }
}
