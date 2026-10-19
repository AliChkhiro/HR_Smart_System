package com.apprh.backend.rgpd.infrastructure;

import com.apprh.backend.rgpd.domain.ErasureRequest;
import com.apprh.backend.rgpd.domain.ErasureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErasureRequestRepository extends JpaRepository<ErasureRequest, Long> {

    Optional<ErasureRequest> findByUserIdAndStatus(Long userId, ErasureStatus status);

    Page<ErasureRequest> findByStatusOrderByRequestedAtAsc(ErasureStatus status, Pageable pageable);

    Page<ErasureRequest> findAllByOrderByRequestedAtDesc(Pageable pageable);

    List<ErasureRequest> findByUserIdOrderByRequestedAtDesc(Long userId);
}
