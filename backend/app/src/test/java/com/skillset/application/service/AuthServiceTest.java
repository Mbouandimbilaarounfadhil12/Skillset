package com.skillset.application.service;

import com.skillset.application.dto.*;
import com.skillset.domain.entity.User;
import com.skillset.domain.entity.UserRole;
import com.skillset.domain.port.CandidateProfileRepositoryPort;
import com.skillset.domain.port.UserRepositoryPort;
import com.skillset.infrastructure.security.AuthorizationService;
import com.skillset.infrastructure.security.JwtUtil;
import com.skillset.infrastructure.util.CloudinaryService;
import com.skillset.infrastructure.util.EmailUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserRepositoryPort   userRepositoryPort;
    @Mock PasswordEncoder      passwordEncoder;
    @Mock JwtUtil              jwtUtil;
    @Mock AuthorizationService authorizationService;
    @Mock CandidateProfileRepositoryPort candidateProfileRepositoryPort;
    @Mock CloudinaryService    cloudinaryService;
    @Mock EmailUtil            emailUtil;

    @InjectMocks AuthService authService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User activeUser() {
        User u = new User();
        u.setId("user-1");
        u.setEmail("alice@example.com");
        u.setPassword("hashed");
        u.setFirstName("Alice");
        u.setLastName("Dupont");
        u.setRole(UserRole.CANDIDATE);
        u.setIsActive(true);
        u.setTwoFactorEnabled(false);
        u.setOnboardingCompleted(false);
        return u;
    }

    private RegisterRequest registerRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setFirstName("Alice");
        r.setLastName("Dupont");
        r.setEmail("alice@example.com");
        r.setPassword("secret");
        r.setRole("CANDIDATE");
        return r;
    }

    // ── register() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("retourne un token JWT après inscription réussie")
        void success_returnsToken() {
            when(emailUtil.isValidEmail("alice@example.com")).thenReturn(true);
            when(userRepositoryPort.existsByEmail("alice@example.com")).thenReturn(false);
            when(passwordEncoder.encode("secret")).thenReturn("hashed");
            User saved = activeUser();
            when(userRepositoryPort.saveUser(any())).thenReturn(saved);
            when(jwtUtil.generateToken("user-1", "CANDIDATE")).thenReturn("jwt-token");

            AuthResponse response = authService.register(registerRequest());

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmail()).isEqualTo("alice@example.com");
            assertThat(response.getRole()).isEqualTo("CANDIDATE");
        }

        @Test
        @DisplayName("lève BAD_REQUEST si l'email est invalide")
        void invalidEmail_throwsBadRequest() {
            when(emailUtil.isValidEmail("alice@example.com")).thenReturn(false);

            assertThatThrownBy(() -> authService.register(registerRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("lève CONFLICT si l'email est déjà utilisé")
        void emailTaken_throwsConflict() {
            when(emailUtil.isValidEmail("alice@example.com")).thenReturn(true);
            when(userRepositoryPort.existsByEmail("alice@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }
    }

    // ── login() ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        private LoginRequest loginRequest() {
            LoginRequest r = new LoginRequest();
            r.setEmail("alice@example.com");
            r.setPassword("secret");
            return r;
        }

        @Test
        @DisplayName("retourne un JWT complet quand la 2FA est désactivée")
        void noTwoFactor_returnsFullJwt() {
            User user = activeUser();
            when(userRepositoryPort.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
            when(jwtUtil.generateToken("user-1", "CANDIDATE")).thenReturn("full-jwt");

            AuthResponse response = authService.login(loginRequest());

            assertThat(response.getToken()).isEqualTo("full-jwt");
            assertThat(response.isTwoFactorRequired()).isFalse();
        }

        @Test
        @DisplayName("retourne preAuthToken et envoie un code par email quand la 2FA est activée")
        void twoFactorEnabled_returnsPreAuthToken() {
            User user = activeUser();
            user.setTwoFactorEnabled(true);
            when(userRepositoryPort.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
            when(jwtUtil.generatePreAuthToken("user-1")).thenReturn("pre-auth-jwt");

            AuthResponse response = authService.login(loginRequest());

            assertThat(response.isTwoFactorRequired()).isTrue();
            assertThat(response.getPreAuthToken()).isEqualTo("pre-auth-jwt");
            assertThat(response.getToken()).isNull();
            verify(emailUtil).sendOtpCode(eq("alice@example.com"), eq("Alice"), anyString());
        }

        @Test
        @DisplayName("lève UNAUTHORIZED si l'utilisateur est introuvable")
        void userNotFound_throwsUnauthorized() {
            when(userRepositoryPort.findByEmail("alice@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("lève UNAUTHORIZED si le mot de passe est incorrect")
        void wrongPassword_throwsUnauthorized() {
            User user = activeUser();
            when(userRepositoryPort.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret", "hashed")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("lève FORBIDDEN si le compte est désactivé")
        void inactiveAccount_throwsForbidden() {
            User user = activeUser();
            user.setIsActive(false);
            when(userRepositoryPort.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ── sendTwoFactorCode() / setup2fa ───────────────────────────────────────

    @Nested
    @DisplayName("sendTwoFactorCode()")
    class SendTwoFactorCode {

        @Test
        @DisplayName("génère un code, le hache et l'envoie par email")
        void success_sendsOtpEmail() {
            User user = activeUser();
            when(userRepositoryPort.findUserById("user-1")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

            authService.sendTwoFactorCode("user-1");

            assertThat(user.getTwoFactorOtpHash()).isEqualTo("hashed-otp");
            assertThat(user.getTwoFactorOtpExpiresAt()).isNotNull();
            verify(emailUtil).sendOtpCode(eq("alice@example.com"), eq("Alice"), anyString());
            verify(userRepositoryPort).saveUser(user);
        }
    }

    // ── confirm2faSetup() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("confirm2faSetup()")
    class Confirm2fa {

        @Test
        @DisplayName("active la 2FA si le code est valide")
        void validCode_enables2fa() {
            User user = activeUser();
            user.setTwoFactorOtpHash("hashed-otp");
            user.setTwoFactorOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
            when(userRepositoryPort.findUserById("user-1")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("123456", "hashed-otp")).thenReturn(true);

            authService.confirm2faSetup("user-1", "123456");

            assertThat(user.getTwoFactorEnabled()).isTrue();
            assertThat(user.getTwoFactorOtpHash()).isNull();
            verify(userRepositoryPort).saveUser(user);
        }

        @Test
        @DisplayName("lève UNAUTHORIZED si le code est invalide")
        void invalidCode_throwsUnauthorized() {
            User user = activeUser();
            user.setTwoFactorOtpHash("hashed-otp");
            user.setTwoFactorOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
            when(userRepositoryPort.findUserById("user-1")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("000000", "hashed-otp")).thenReturn(false);

            assertThatThrownBy(() -> authService.confirm2faSetup("user-1", "000000"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("lève UNAUTHORIZED si le code a expiré")
        void expiredCode_throwsUnauthorized() {
            User user = activeUser();
            user.setTwoFactorOtpHash("hashed-otp");
            user.setTwoFactorOtpExpiresAt(LocalDateTime.now().minusMinutes(1));
            when(userRepositoryPort.findUserById("user-1")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.confirm2faSetup("user-1", "123456"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("lève CONFLICT si la 2FA est déjà activée")
        void alreadyEnabled_throwsConflict() {
            User user = activeUser();
            user.setTwoFactorEnabled(true);
            when(userRepositoryPort.findUserById("user-1")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.confirm2faSetup("user-1", "123456"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }
    }

    // ── verifyOtpLogin() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyOtpLogin()")
    class VerifyOtpLogin {

        @Test
        @DisplayName("retourne un JWT complet après vérification du code réussie")
        void validCode_returnsFullJwt() {
            User user = activeUser();
            user.setTwoFactorEnabled(true);
            user.setTwoFactorOtpHash("hashed-otp");
            user.setTwoFactorOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
            TwoFactorLoginRequest req = new TwoFactorLoginRequest();
            req.setPreAuthToken("pre-auth-jwt");
            req.setCode("123456");

            when(jwtUtil.getUserIdFromPreAuthToken("pre-auth-jwt")).thenReturn("user-1");
            when(userRepositoryPort.findUserById("user-1")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("123456", "hashed-otp")).thenReturn(true);
            when(jwtUtil.generateToken("user-1", "CANDIDATE")).thenReturn("full-jwt");

            AuthResponse response = authService.verifyOtpLogin(req);

            assertThat(response.getToken()).isEqualTo("full-jwt");
            assertThat(user.getTwoFactorOtpHash()).isNull();
        }

        @Test
        @DisplayName("lève UNAUTHORIZED si le pre-auth token est invalide")
        void invalidPreAuthToken_throwsUnauthorized() {
            TwoFactorLoginRequest req = new TwoFactorLoginRequest();
            req.setPreAuthToken("bad-token");
            req.setCode("123456");
            when(jwtUtil.getUserIdFromPreAuthToken("bad-token"))
                    .thenThrow(new RuntimeException("token invalide"));

            assertThatThrownBy(() -> authService.verifyOtpLogin(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("lève UNAUTHORIZED si le code est incorrect")
        void invalidCode_throwsUnauthorized() {
            User user = activeUser();
            user.setTwoFactorEnabled(true);
            user.setTwoFactorOtpHash("hashed-otp");
            user.setTwoFactorOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
            TwoFactorLoginRequest req = new TwoFactorLoginRequest();
            req.setPreAuthToken("pre-auth-jwt");
            req.setCode("000000");

            when(jwtUtil.getUserIdFromPreAuthToken("pre-auth-jwt")).thenReturn("user-1");
            when(userRepositoryPort.findUserById("user-1")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("000000", "hashed-otp")).thenReturn(false);

            assertThatThrownBy(() -> authService.verifyOtpLogin(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── disable2fa() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("disable2fa()")
    class Disable2fa {

        @Test
        @DisplayName("désactive la 2FA si le code est valide")
        void validCode_disables2fa() {
            User user = activeUser();
            user.setTwoFactorEnabled(true);
            user.setTwoFactorOtpHash("hashed-otp");
            user.setTwoFactorOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
            when(userRepositoryPort.findUserById("user-1")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("123456", "hashed-otp")).thenReturn(true);

            authService.disable2fa("user-1", "123456");

            assertThat(user.getTwoFactorEnabled()).isFalse();
            assertThat(user.getTwoFactorOtpHash()).isNull();
        }

        @Test
        @DisplayName("lève BAD_REQUEST si la 2FA n'est pas activée")
        void notEnabled_throwsBadRequest() {
            User user = activeUser();
            when(userRepositoryPort.findUserById("user-1")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.disable2fa("user-1", "123456"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("lève UNAUTHORIZED si le code est invalide")
        void invalidCode_throwsUnauthorized() {
            User user = activeUser();
            user.setTwoFactorEnabled(true);
            user.setTwoFactorOtpHash("hashed-otp");
            user.setTwoFactorOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
            when(userRepositoryPort.findUserById("user-1")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("000000", "hashed-otp")).thenReturn(false);

            assertThatThrownBy(() -> authService.disable2fa("user-1", "000000"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
