package com.skillset.application.service;

import com.skillset.domain.entity.JobListing;
import com.skillset.domain.entity.JobBoardPublication;
import com.skillset.domain.port.JobBoardAdapter;
import com.skillset.domain.value.JobBoardPartner;
import com.skillset.domain.value.JobBoardPublishResult;
import com.skillset.infrastructure.persistence.JobListingRepository;
import com.skillset.infrastructure.persistence.JobBoardPublicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Service orchestrateur pour la publication multi-partenaire.
 * Coordonne l'appel parallèle à plusieurs job boards et persiste les résultats.
 *
 * Principes :
 * - Appels parallèles (CompletableFuture) pour performance
 * - Échec d'un partenaire n'affecte pas les autres
 * - Résultats persistés dans job_board_publications
 * - Rapport consolidé retourné au contrôleur
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobBoardPublishingService {

    private final JobBoardRouter jobBoardRouter;
    private final JobListingRepository jobListingRepository;
    private final JobBoardPublicationRepository jobBoardPublicationRepository;
    private final Map<JobBoardPartner, JobBoardAdapter> adapterRegistry;

    /**
     * Publie une offre sur tous les job boards configurés pour les pays cibles.
     *
     * @param jobId ID de l'offre
     * @param targetCountries codes ISO des pays cibles
     * @return rapport consolidé de publication
     */
    @Transactional
    public PublishingReport publishToJobBoards(String jobId, List<String> targetCountries) {
        log.info("Starting job board publication for job {} to countries {}", jobId, targetCountries);

        PublishingReport report = new PublishingReport(jobId);

        // 1. Charger l'offre
        JobListing jobListing = jobListingRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job listing not found: " + jobId));

        // 2. Déterminer les partenaires cibles
        List<JobBoardPartner> targetPartners = jobBoardRouter.determineTargetPartners(targetCountries);
        if (targetPartners.isEmpty()) {
            log.warn("No job board partners found for countries {}", targetCountries);
            report.addResult(null, JobBoardPublishResult.failure(null, "No partners configured for target countries"));
            return report;
        }

        // 3. Lancer les appels en parallèle
        Map<JobBoardPartner, CompletableFuture<JobBoardPublishResult>> futures = new ConcurrentHashMap<>();

        for (JobBoardPartner partner : targetPartners) {
            JobBoardAdapter adapter = adapterRegistry.get(partner);

            if (adapter == null) {
                log.warn("No adapter found for partner {}", partner);
                report.addResult(partner, JobBoardPublishResult.failure(partner, "Adapter not available"));
                continue;
            }

            if (!adapter.isAvailable()) {
                log.warn("Partner {} not available (credentials not configured)", partner);
                report.addResult(partner, JobBoardPublishResult.failure(partner, "Credentials not configured"));
                continue;
            }

            CompletableFuture<JobBoardPublishResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("Publishing to {} for job {}", partner, jobId);
                    return adapter.publish(jobListing);
                } catch (Exception e) {
                    log.error("Unexpected error publishing to {}: {}", partner, e.getMessage(), e);
                    return JobBoardPublishResult.failure(partner, "Unexpected error: " + e.getMessage());
                }
            });

            futures.put(partner, future);
        }

        // 4. Attendre tous les résultats (sans bloquer sur un échec)
        futures.forEach((partner, future) -> {
            try {
                JobBoardPublishResult result = future.get();
                report.addResult(partner, result);

                // Persister le résultat en base de données
                persistPublicationResult(jobId, result);
            } catch (Exception e) {
                log.error("Error waiting for result from {}: {}", partner, e.getMessage(), e);
                report.addResult(partner, JobBoardPublishResult.failure(partner, e.getMessage()));
            }
        });

        log.info("Job board publication completed for job {}. Results: {}", jobId, report.getResults());
        return report;
    }

    /**
     * Dépublie une offre de tous les job boards où elle a été publiée.
     *
     * @param jobId ID de l'offre
     * @return rapport de dépublication
     */
    @Transactional
    public PublishingReport unpublishFromJobBoards(String jobId) {
        log.info("Starting job board unpublication for job {}", jobId);

        PublishingReport report = new PublishingReport(jobId);

        // Charger toutes les publications existantes pour cette offre
        List<JobBoardPublication> publications = jobBoardPublicationRepository.findByJobListingId(jobId);

        if (publications.isEmpty()) {
            log.info("Job {} has no publications, nothing to unpublish", jobId);
            return report;
        }

        // Dépublier en parallèle
        Map<JobBoardPartner, CompletableFuture<Boolean>> futures = new ConcurrentHashMap<>();

        for (JobBoardPublication pub : publications) {
            JobBoardPartner partner = pub.getPartner();
            JobBoardAdapter adapter = adapterRegistry.get(partner);

            if (adapter == null) {
                log.warn("No adapter found for partner {}", partner);
                report.addResult(partner, JobBoardPublishResult.failure(partner, "Adapter not available"));
                continue;
            }

            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("Unpublishing from {} for job {}", partner, jobId);
                    return adapter.unpublish(pub.getExternalId());
                } catch (Exception e) {
                    log.error("Unexpected error unpublishing from {}: {}", partner, e.getMessage(), e);
                    return false;
                }
            });

            futures.put(partner, future);
        }

        // Attendre les résultats et mettre à jour les publications
        futures.forEach((partner, future) -> {
            try {
                boolean success = future.get();
                if (success) {
                    jobBoardPublicationRepository.markAsUnpublished(jobId, partner);
                    report.addResult(partner, JobBoardPublishResult.unpublished(partner));
                } else {
                    report.addResult(partner, JobBoardPublishResult.failure(partner, "Unpublish failed"));
                }
            } catch (Exception e) {
                log.error("Error waiting for unpublish result from {}: {}", partner, e.getMessage(), e);
                report.addResult(partner, JobBoardPublishResult.failure(partner, e.getMessage()));
            }
        });

        log.info("Job board unpublication completed for job {}. Results: {}", jobId, report.getResults());
        return report;
    }

    /**
     * Persiste le résultat d'une publication en base de données.
     */
    private void persistPublicationResult(String jobId, JobBoardPublishResult result) {
        try {
            JobBoardPublication publication = JobBoardPublication.builder()
                .jobListingId(jobId)
                .partner(result.getPartner())
                .status(result.getStatus().name())
                .externalId(result.getExternalId())
                .externalUrl(result.getExternalUrl())
                .errorMessage(result.getErrorMessage())
                .publishedAt(LocalDateTime.now())
                .build();

            jobBoardPublicationRepository.save(publication);
            log.debug("Persisted publication result for job {} on {}", jobId, result.getPartner());
        } catch (Exception e) {
            log.error("Error persisting publication result: {}", e.getMessage(), e);
        }
    }

    // ==================== DTO ====================

    /**
     * Rapport consolidé de publication/dépublication.
     */
    public static class PublishingReport {
        private final String jobId;
        private final List<JobBoardPublishResult> results;
        private final LocalDateTime timestamp;

        public PublishingReport(String jobId) {
            this.jobId = jobId;
            this.results = new CopyOnWriteArrayList<>();
            this.timestamp = LocalDateTime.now();
        }

        public void addResult(JobBoardPartner partner, JobBoardPublishResult result) {
            if (result != null) {
                results.add(result);
            }
        }

        public String getJobId() {
            return jobId;
        }

        public List<JobBoardPublishResult> getResults() {
            return results;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public boolean allSuccess() {
            return results.stream().allMatch(JobBoardPublishResult::isSuccess);
        }

        public boolean anySuccess() {
            return results.stream().anyMatch(JobBoardPublishResult::isSuccess);
        }

        public int getSuccessCount() {
            return (int) results.stream().filter(JobBoardPublishResult::isSuccess).count();
        }

        public int getFailureCount() {
            return (int) results.stream().filter(JobBoardPublishResult::isFailed).count();
        }

        @Override
        public String toString() {
            return "PublishingReport{" +
                    "jobId='" + jobId + '\'' +
                    ", successCount=" + getSuccessCount() +
                    ", failureCount=" + getFailureCount() +
                    ", totalCount=" + results.size() +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}
