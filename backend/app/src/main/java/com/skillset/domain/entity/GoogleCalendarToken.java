package com.skillset.domain.entity;

import com.skillset.infrastructure.util.EncryptionConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "google_calendar_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "access_token", columnDefinition = "TEXT")
    @Convert(converter = EncryptionConverter.class)
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    @Convert(converter = EncryptionConverter.class)
    private String refreshToken;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "calendar_id")
    private String calendarId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
