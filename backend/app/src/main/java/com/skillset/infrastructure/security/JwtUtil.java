package com.skillset.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private static final String ROLE_CLAIM      = "role";
    private static final String SCOPE_CLAIM     = "scope";
    private static final String PRE_AUTH_SCOPE  = "pre-auth";
    private static final long   PRE_AUTH_TTL_MS = 5 * 60 * 1000L;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpirationMs;

    @PostConstruct
    void validateConfiguration() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET est obligatoire. Définissez la variable d'environnement JWT_SECRET "
                            + "ou ajoutez-la dans un fichier .env (voir .env.example).");
        }
        if (jwtSecret.getBytes().length < 64) {
            throw new IllegalStateException(
                    "JWT_SECRET doit contenir au moins 64 caractères pour l'algorithme HS512.");
        }
    }

    public String generateToken(String userId, String role) {
        SecretKey key = signingKey();
        return Jwts.builder()
                .setSubject(userId)
                .claim(ROLE_CLAIM, role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get(ROLE_CLAIM, String.class);
    }

    public String generatePreAuthToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .claim(SCOPE_CLAIM, PRE_AUTH_SCOPE)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + PRE_AUTH_TTL_MS))
                .signWith(signingKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUserIdFromPreAuthToken(String token) {
        Claims claims = parseClaims(token);
        if (!PRE_AUTH_SCOPE.equals(claims.get(SCOPE_CLAIM))) {
            throw new IllegalArgumentException("Token invalide : scope incorrect");
        }
        return claims.getSubject();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
