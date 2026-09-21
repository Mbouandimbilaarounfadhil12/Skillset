package com.skillset.domain.value;

/**
 * Résultat d'une tentative de publication sur un job board partenaire.
 * Value Object immuable représentant le statut et les détails de la publication.
 */
public class JobBoardPublishResult {
    public enum Status {
        PUBLISHED,
        FAILED,
        UNPUBLISHED
    }

    private final JobBoardPartner partner;
    private final Status status;
    private final String externalId;        // ID généré par le partenaire
    private final String externalUrl;       // URL publique de l'offre chez le partenaire
    private final String errorMessage;      // Message d'erreur si status = FAILED

    private JobBoardPublishResult(JobBoardPartner partner, Status status, String externalId,
                                  String externalUrl, String errorMessage) {
        this.partner = partner;
        this.status = status;
        this.externalId = externalId;
        this.externalUrl = externalUrl;
        this.errorMessage = errorMessage;
    }

    // Constructeurs statiques pour plus de clarté

    public static JobBoardPublishResult success(JobBoardPartner partner, String externalId, String externalUrl) {
        return new JobBoardPublishResult(partner, Status.PUBLISHED, externalId, externalUrl, null);
    }

    public static JobBoardPublishResult failure(JobBoardPartner partner, String errorMessage) {
        return new JobBoardPublishResult(partner, Status.FAILED, null, null, errorMessage);
    }

    public static JobBoardPublishResult unpublished(JobBoardPartner partner) {
        return new JobBoardPublishResult(partner, Status.UNPUBLISHED, null, null, null);
    }

    // Getters

    public JobBoardPartner getPartner() {
        return partner;
    }

    public Status getStatus() {
        return status;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return status == Status.PUBLISHED;
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }

    @Override
    public String toString() {
        return "JobBoardPublishResult{" +
                "partner=" + partner +
                ", status=" + status +
                ", externalId='" + externalId + '\'' +
                ", externalUrl='" + externalUrl + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
