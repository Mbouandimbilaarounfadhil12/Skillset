package com.skillset.domain.entity;

import com.skillset.domain.value.JobBoardPartner;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Configuration de routage pour les intégrations de job boards.
 * Mappe pays → partenaire job board avec priorité et statut actif/inactif.
 *
 * Exemple de données :
 * - FR → FRANCE_TRAVAIL (priority 1)
 * - FR → LINKEDIN (priority 2)
 * - CM → BRIGHTERMONDAY (priority 1)
 * - CM → LINKEDIN (priority 2)
 * - * (wildcard) → LINKEDIN (priority 1, fallback universel)
 */
@Entity
@Table(name = "job_board_configs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"country_code", "partner"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobBoardConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Code ISO du pays (FR, CM, SN, CI, etc.) ou "*" pour fallback universel.
     */
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    /**
     * Partenaire job board ciblé.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "partner", nullable = false)
    private JobBoardPartner partner;

    /**
     * Si true, ce partenaire est utilisé pour ce pays.
     * Si false, le routage l'ignore.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Ordre d'appel en cas de multiples partenaires pour un même pays.
     * Priority 1 = appelé en premier, puis 2, puis 3, etc.
     * Permet d'appeler les partenaires en parallèle ou en série selon la priorité.
     */
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 1;

    /**
     * Métadonnées optionnelles pour ce mapping (JSON).
     * Ex: {"region": "West Africa", "supportedLanguages": ["en", "fr"]}
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
