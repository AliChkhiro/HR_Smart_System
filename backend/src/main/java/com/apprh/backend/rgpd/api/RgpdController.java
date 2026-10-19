package com.apprh.backend.rgpd.api;

import com.apprh.backend.common.security.UserPrincipal;
import com.apprh.backend.rgpd.application.RgpdService;
import com.apprh.backend.rgpd.domain.ErasureStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Conformite RGPD : export des donnees personnelles (droit d'acces) et
 * demandes d'effacement de compte (droit a l'effacement).
 */
@RestController
@RequestMapping("/rgpd")
@RequiredArgsConstructor
public class RgpdController {

    private final RgpdService rgpdService;

    /** Droit d'acces : export JSON des donnees personnelles de l'utilisateur connecte. */
    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public PersonalDataExport export(Authentication authentication) {
        return rgpdService.export(principal(authentication));
    }

    /** Droit a l'effacement : l'utilisateur depose une demande. */
    @PostMapping("/erasure-request")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ErasureRequestResponse requestErasure(Authentication authentication,
                                                 @Valid @RequestBody(required = false) ErasureRequestCreate request) {
        String reason = request == null ? null : request.reason();
        return rgpdService.requestErasure(principal(authentication), reason);
    }

    /** Demandes d'effacement de l'utilisateur connecte. */
    @GetMapping("/my-erasure-requests")
    @PreAuthorize("isAuthenticated()")
    public List<ErasureRequestResponse> myErasureRequests(Authentication authentication) {
        return rgpdService.myErasureRequests(principal(authentication));
    }

    /** Consultation (administrateur) des demandes d'effacement. */
    @GetMapping("/erasure-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ErasureRequestResponse> listErasureRequests(
            @RequestParam(required = false) ErasureStatus status,
            @PageableDefault(size = 50) Pageable pageable) {
        return rgpdService.listErasureRequests(status, pageable);
    }

    /** Validation (administrateur) d'une demande d'effacement. */
    @PostMapping("/erasure-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ErasureRequestResponse approve(Authentication authentication, @PathVariable Long id) {
        return rgpdService.approve(id, principal(authentication).id());
    }

    /** Refus (administrateur) d'une demande d'effacement. */
    @PostMapping("/erasure-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ErasureRequestResponse reject(Authentication authentication, @PathVariable Long id,
                                         @Valid @RequestBody(required = false) ErasureDecisionRequest request) {
        String comment = request == null ? null : request.comment();
        return rgpdService.reject(id, principal(authentication).id(), comment);
    }

    private UserPrincipal principal(Authentication authentication) {
        return (UserPrincipal) authentication.getPrincipal();
    }
}
