package com.skillset.application.service;

import com.skillset.application.dto.*;
import com.skillset.domain.entity.CandidateProfile;
import com.skillset.domain.entity.User;
import com.skillset.domain.entity.UserRole;
import com.skillset.domain.port.CandidateProfileRepositoryPort;
import com.skillset.domain.port.UserRepositoryPort;
import com.skillset.infrastructure.security.AuthorizationService;
import com.skillset.infrastructure.security.JwtUtil;
import com.skillset.infrastructure.util.CloudinaryService;
import com.skillset.infrastructure.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final UserRepositoryPort   userRepositoryPort;
    private final PasswordEncoder      passwordEncoder;
    private final JwtUtil              jwtUtil;
    private final AuthorizationService authorizationService;
    private final CandidateProfileRepositoryPort candidateProfileRepositoryPort;
    private final CloudinaryService    cloudinaryService;
    private final EmailUtil            emailUtil;

    // ── Inscription ──────────────────────────────────────────────────────────

    public AuthResponse register(RegisterRequest request) {
        if (!emailUtil.isValidEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse email invalide.");
        }
        if (userRepositoryPort.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé.");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.valueOf(request.getRole()));
        user.setIsActive(true);
        user.setTwoFactorEnabled(false);

        User saved = userRepositoryPort.saveUser(user);
        String token = jwtUtil.generateToken(saved.getId(), saved.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .id(saved.getId())
                .email(saved.getEmail())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .role(saved.getRole().toString())
                .onboardingCompleted(Boolean.TRUE.equals(saved.getOnboardingCompleted()))
                .build();
    }

    // ── Connexion ─────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        User user = userRepositoryPort.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
        }

        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte est désactivé.");
        }

        // 2FA activée : on envoie un code par email et on retourne un token temporaire (pre-auth)
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            sendTwoFactorCode(user);
            return AuthResponse.builder()
                    .twoFactorRequired(true)
                    .preAuthToken(jwtUtil.generatePreAuthToken(user.getId()))
                    .build();
        }

        return buildFullResponse(user);
    }

    // ── 2FA — Configuration ───────────────────────────────────────────────────

    /**
     * Envoie un code de vérification par email. Utilisé à la fois pour activer la 2FA
     * (premier code) et avant une désactivation (un code frais est nécessaire).
     */
    public void sendTwoFactorCode(String userId) {
        sendTwoFactorCode(findUser(userId));
    }

    private void sendTwoFactorCode(User user) {
        String code = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        user.setTwoFactorOtpHash(passwordEncoder.encode(code));
        user.setTwoFactorOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepositoryPort.saveUser(user);
        emailUtil.sendOtpCode(user.getEmail(), user.getFirstName(), code);
    }

    private boolean verifyTwoFactorCode(User user, String code) {
        if (user.getTwoFactorOtpHash() == null || user.getTwoFactorOtpExpiresAt() == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(user.getTwoFactorOtpExpiresAt())) {
            return false;
        }
        return passwordEncoder.matches(code, user.getTwoFactorOtpHash());
    }

    private void clearTwoFactorCode(User user) {
        user.setTwoFactorOtpHash(null);
        user.setTwoFactorOtpExpiresAt(null);
    }

    public void confirm2faSetup(String userId, String code) {
        User user = findUser(userId);

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "L'authentification à deux facteurs est déjà activée.");
        }
        if (!verifyTwoFactorCode(user, code)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Code invalide ou expiré.");
        }

        user.setTwoFactorEnabled(true);
        clearTwoFactorCode(user);
        userRepositoryPort.saveUser(user);
    }

    // ── 2FA — Vérification au login ───────────────────────────────────────────

    public AuthResponse verifyOtpLogin(TwoFactorLoginRequest request) {
        String userId;
        try {
            userId = jwtUtil.getUserIdFromPreAuthToken(request.getPreAuthToken());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Token de pré-authentification invalide ou expiré.");
        }

        User user = findUser(userId);

        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La 2FA n'est pas activée sur ce compte.");
        }
        if (!verifyTwoFactorCode(user, request.getCode())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Code invalide ou expiré.");
        }

        clearTwoFactorCode(user);
        userRepositoryPort.saveUser(user);
        return buildFullResponse(user);
    }

    // ── 2FA — Désactivation ───────────────────────────────────────────────────

    public void disable2fa(String userId, String code) {
        User user = findUser(userId);

        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La 2FA n'est pas activée sur ce compte.");
        }
        if (!verifyTwoFactorCode(user, code)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Code invalide ou expiré. La 2FA n'a pas été désactivée.");
        }

        user.setTwoFactorEnabled(false);
        clearTwoFactorCode(user);
        userRepositoryPort.saveUser(user);
    }

    // ── Profil utilisateur ────────────────────────────────────────────────────

    public UserDTO getUserProfile(String currentUserId, String userId) {
        authorizationService.requireSelfOrAdmin(currentUserId, userId);
        Optional<User> user = userRepositoryPort.findUserById(userId);
        return user.map(this::toDTO).orElse(null);
    }

    public UserDTO updateUser(String currentUserId, String userId, UserDTO details) {
        authorizationService.requireSelfOrAdmin(currentUserId, userId);
        return userRepositoryPort.findUserById(userId).map(user -> {
            if (details.getFirstName() != null) user.setFirstName(details.getFirstName());
            if (details.getLastName() != null) user.setLastName(details.getLastName());
            if (details.getPhoneNumber() != null) user.setPhoneNumber(details.getPhoneNumber());
            if (details.getProfilePictureUrl() != null) user.setProfilePictureUrl(details.getProfilePictureUrl());
            User saved = userRepositoryPort.saveUser(user);

            if (saved.getRole() == UserRole.CANDIDATE) {
                CandidateProfile profile = candidateProfileRepositoryPort.findByUserId(userId)
                        .orElseGet(() -> {
                            CandidateProfile p = new CandidateProfile();
                            p.setUserId(userId);
                            return p;
                        });
                if (details.getJobDomain() != null) profile.setJobDomain(details.getJobDomain());
                if (details.getDesiredRole() != null) profile.setDesiredRole(details.getDesiredRole());
                if (details.getExperienceLevel() != null) profile.setExperienceLevel(details.getExperienceLevel());
                if (details.getContractType() != null) profile.setContractType(details.getContractType());
                if (details.getLocation() != null) profile.setLocation(details.getLocation());
                if (details.getSkills() != null) profile.setSkills(details.getSkills());
                if (details.getBio() != null) profile.setBio(details.getBio());
                candidateProfileRepositoryPort.save(profile);
            }

            return toDTO(saved);
        }).orElse(null);
    }

    public UserDTO updateProfilePhoto(String currentUserId, String userId, MultipartFile file) {
        authorizationService.requireSelfOrAdmin(currentUserId, userId);
        User user = findUser(userId);
        String url = cloudinaryService.uploadImage(file);
        if (url == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Le stockage des images n'est pas configuré sur ce serveur.");
        }
        user.setProfilePictureUrl(url);
        return toDTO(userRepositoryPort.saveUser(user));
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    private User findUser(String userId) {
        return userRepositoryPort.findUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    private AuthResponse buildFullResponse(User user) {
        emailUtil.sendLoginNotification(user.getEmail(), user.getFirstName());

        return AuthResponse.builder()
                .token(jwtUtil.generateToken(user.getId(), user.getRole().name()))
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().toString())
                .onboardingCompleted(Boolean.TRUE.equals(user.getOnboardingCompleted()))
                .build();
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getPhoneNumber(),
                user.getRole().toString(), user.getProfilePictureUrl());

        if (user.getRole() == UserRole.CANDIDATE) {
            candidateProfileRepositoryPort.findByUserId(user.getId()).ifPresent(profile -> {
                dto.setJobDomain(profile.getJobDomain());
                dto.setDesiredRole(profile.getDesiredRole());
                dto.setExperienceLevel(profile.getExperienceLevel());
                dto.setContractType(profile.getContractType());
                dto.setLocation(profile.getLocation());
                dto.setSkills(profile.getSkills());
                dto.setBio(profile.getBio());
            });
        }

        return dto;
    }
}
