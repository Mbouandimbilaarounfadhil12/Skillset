package com.skillset.domain.port;

import com.skillset.domain.entity.JobListing;
import com.skillset.domain.value.JobBoardPartner;
import com.skillset.domain.value.JobBoardPublishResult;

/**
 * Port (interface) définissant le contrat pour tout adaptateur de job board.
 * Chaque implémentation publie une offre sur un partenaire spécifique
 * (France Travail, LinkedIn, BrighterMonday, Jobartisan, etc.)
 */
public interface JobBoardAdapter {

    /**
     * Retourne le partenaire job board que cet adaptateur gère.
     */
    JobBoardPartner getPartner();

    /**
     * Publie une offre d'emploi sur le job board partenaire.
     *
     * @param jobListing l'offre d'emploi à publier
     * @return résultat de la publication (succès/échec, ID externe, URL, message d'erreur)
     */
    JobBoardPublishResult publish(JobListing jobListing);

    /**
     * Dépublie (retire) une offre du job board partenaire.
     *
     * @param externalId l'ID externe fourni par le partenaire lors de la publication
     * @return true si succès, false sinon
     */
    boolean unpublish(String externalId);

    /**
     * Vérifie que le service est disponible/configuré.
     * Utile pour savoir si les credentials sont en place avant d'appeler publish().
     *
     * @return true si le service peut être utilisé
     */
    boolean isAvailable();
}
