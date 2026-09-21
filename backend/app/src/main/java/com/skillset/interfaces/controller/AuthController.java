package com.skillset.interfaces.controller;

import com.skillset.application.dto.*;
import com.skillset.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── Inscription / Connexion ───────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── Profil ────────────────────────────────────────────────────────────────

    @GetMapping("/profile/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<UserDTO> getProfile(@AuthenticationPrincipal String currentUserId,
                                              @PathVariable String userId) {
        UserDTO dto = authService.getUserProfile(currentUserId, userId);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PutMapping("/profile/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<UserDTO> updateProfile(@AuthenticationPrincipal String currentUserId,
                                                 @PathVariable String userId,
                                                 @RequestBody UserDTO details) {
        UserDTO dto = authService.updateUser(currentUserId, userId, details);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/profile/{userId}/photo", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<UserDTO> uploadProfilePhoto(@AuthenticationPrincipal String currentUserId,
                                                       @PathVariable String userId,
                                                       @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(authService.updateProfilePhoto(currentUserId, userId, file));
    }

    // ── 2FA — Configuration (utilisateur authentifié) ─────────────────────────
    // Un code à 6 chiffres est envoyé par email : à l'activation (setup) et avant
    // toute désactivation (un code frais est requis dans les deux cas).

    @PostMapping("/2fa/setup")
    public ResponseEntity<Void> setup2fa(
            @AuthenticationPrincipal String currentUserId) {
        authService.sendTwoFactorCode(currentUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<Void> confirm2fa(
            @AuthenticationPrincipal String currentUserId,
            @RequestBody TotpCodeRequest request) {
        authService.confirm2faSetup(currentUserId, request.getCode());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/2fa/disable")
    public ResponseEntity<Void> disable2fa(
            @AuthenticationPrincipal String currentUserId,
            @RequestBody TotpCodeRequest request) {
        authService.disable2fa(currentUserId, request.getCode());
        return ResponseEntity.ok().build();
    }

    // ── 2FA — Vérification au login (endpoint public, pre-auth token) ─────────

    @PostMapping("/2fa/verify-login")
    public ResponseEntity<AuthResponse> verifyOtpLogin(
            @RequestBody TwoFactorLoginRequest request) {
        return ResponseEntity.ok(authService.verifyOtpLogin(request));
    }
}
