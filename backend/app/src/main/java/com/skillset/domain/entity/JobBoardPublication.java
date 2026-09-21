package com.skillset.domain.entity;

import com.skillset.domain.value.JobBoardPartner;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Enregistrement d'une tentative de publication d'une offre sur un job board partenaire.
 * Permet de tracker l'historique de publication et de la relation avec les partenaires.
 */
@Entity
@Table(name = "job_board_publications", indexes = {
    @Index(name = "idx_job_listing_id", columnList = "job_listing_id"),
    @Index(name = "idx_partner", columnList = "partner"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_published_at", columnList = "published_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobBoardPublication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Référence à l'offre d'emploi publiée.
     */
    @Column(name = "job_listing_id", nullable = false)
    private String jobListingId;

    /**
     * Partenaire job board où l'offre a été publiée.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "partner", nullable = false)
    private JobBoardPartner partner;

    /**
     * Status de la publication : PUBLISHED, FAILED, UNPUBLISHED.
     */
    @Column(name = "status", nullable = false)
    private String status; // PUBLISHED / FAILED / UNPUBLISHED

    /**
     * ID généré par le partenaire pour identifier l'offre sur sa plateforme.
     * Ex: ID France Travail, LinkedIn Job ID, etc.
     */
    @Column(name = "external_id")
    private String externalId;

    /**
     * URL publique de l'offre sur le site du partenaire.
     * Ex: https://www.francetravail.fr/offres/xyz123
     */
    @Column(name = "external_url")
    private String externalUrl;

    /**
     * Message d'erreur si status = FAILED.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Timestamp de la publication.
     */
    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    /**
     * Timestamp de la dernière mise à jour (dépublication, etc.).
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
