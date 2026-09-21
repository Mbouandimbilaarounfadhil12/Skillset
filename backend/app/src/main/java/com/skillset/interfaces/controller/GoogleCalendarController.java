package com.skillset.interfaces.controller;

import com.skillset.domain.entity.GoogleCalendarToken;
import com.skillset.infrastructure.integration.GoogleCalendarService;
import com.skillset.infrastructure.persistence.GoogleCalendarTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/google")
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarController {
    private final GoogleCalendarService googleCalendarService;
    private final GoogleCalendarTokenRepository tokenRepository;

    @GetMapping("/auth")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl(@AuthenticationPrincipal String userId) {
        try {
            UUID userUUID = UUID.fromString(userId);
            String authUrl = googleCalendarService.getAuthorizationUrl(userUUID);
            Map<String, String> response = new HashMap<>();
            response.put("authUrl", authUrl);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallback(
            @RequestParam String code,
            @RequestParam String state,
            @AuthenticationPrincipal String userId) {
        try {
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            UUID userUUID = UUID.fromString(userId);
            GoogleCalendarToken token = googleCalendarService.handleOAuthCallback(code, userUUID);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Google Calendar integration successful");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Invalid user ID format");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error handling OAuth callback: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "OAuth callback handling failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Map<String, Object>> getConnectionStatus(@AuthenticationPrincipal String userId) {
        try {
            UUID userUUID = UUID.fromString(userId);
            Optional<GoogleCalendarToken> tokenOpt = tokenRepository.findByUserId(userUUID);

            Map<String, Object> response = new HashMap<>();
            response.put("connected", tokenOpt.isPresent());

            if (tokenOpt.isPresent()) {
                GoogleCalendarToken token = tokenOpt.get();
                response.put("connectedAt", token.getCreatedAt());
                response.put("expiresAt", token.getExpiresAt());
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Void> disconnectGoogleCalendar(@AuthenticationPrincipal String userId) {
        try {
            UUID userUUID = UUID.fromString(userId);
            tokenRepository.deleteByUserId(userUUID);
            log.info("Google Calendar token deleted for user: {}", userUUID);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error deleting Google Calendar token: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
