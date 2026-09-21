package com.skillset.interfaces.controller;

import com.skillset.application.service.GdprService;
import com.skillset.domain.entity.ConsentRecord;
import com.skillset.domain.entity.ConsentRecord.ConsentType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gdpr")
@RequiredArgsConstructor
public class GdprController {

    private final GdprService gdprService;

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> exportUserData(@AuthenticationPrincipal String userId) {
        gdprService.exportUserData(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> eraseUserData(@AuthenticationPrincipal String userId) {
        gdprService.eraseUserData(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/consents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConsentRecord>> getUserConsents(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(gdprService.getUserConsents(userId));
    }

    @PutMapping("/consents/{type}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateConsent(
            @AuthenticationPrincipal String userId,
            @PathVariable ConsentType type,
            @RequestBody Map<String, Boolean> body,
            HttpServletRequest request) {
        boolean accepted = Boolean.TRUE.equals(body.get("accepted"));
        gdprService.updateConsent(userId, type, accepted, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/consents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> revokeAllConsents(@AuthenticationPrincipal String userId) {
        gdprService.revokeAllConsents(userId);
        return ResponseEntity.ok().build();
    }
}
