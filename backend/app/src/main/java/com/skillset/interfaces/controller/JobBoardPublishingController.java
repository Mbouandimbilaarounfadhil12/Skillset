package com.skillset.interfaces.controller;

import com.skillset.application.service.JobBoardPublishingService;
import com.skillset.application.service.JobBoardPublishingService.PublishingReport;
import com.skillset.domain.entity.JobListing;
import com.skillset.domain.value.JobBoardPublishResult;
import com.skillset.infrastructure.persistence.JobListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrôleur REST pour la publication multi-régionale d'offres d'emploi.
 *
 * Endpoints:
 * - POST /api/jobboards/publish/{jobId} — Publie sur tous les job boards configurés pour les pays cibles
 * - DELETE /api/jobboards/unpublish/{jobId} — Dépublie de tous les job boards
 *
 * Rétrocompatibilité:
 * - Les anciens endpoints /api/jobboards/france-travail/* continuent de fonctionner via redirection
 */
@RestController
@RequestMapping("/api/jobboards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER')")
@Slf4j
public class JobBoardPublishingController {

    private final JobBoardPublishingService jobBoardPublishingService;
    private final JobListingRepository jobListingRepository;

    /**
     * Publie une offre sur tous les job boards configurés pour les pays cibles.
     *
     * @param jobId ID de l'offre
     * @param request corps contenant targetCountries (codes ISO)
     * @param userId utilisateur authentifié (vérification d'ownership)
     * @return rapport de publication avec résultats par partenaire
     *
     * Exemple requête:
     * POST /api/jobboards/publish/job-uuid-123
     * {
     *   "targetCountries": ["CM", "FR"]
     * }
     *
     * Exemple réponse (201 Created):
     * {
     *   "jobId": "job-uuid-123",
     *   "timestamp": "2024-01-15T10:30:00",
     *   "successCount": 3,
     *   "failureCount": 0,
     *   "totalCount": 3,
     *   "results": [
     *     {
     *       "partner": "FRANCE_TRAVAIL",
     *       "status": "PUBLISHED",
     *       "externalId": "ft-12345",
     *       "externalUrl": "https://www.francetravail.fr/offres/ft-12345"
     *     },
     *     {
     *       "partner": "BRIGHTERMONDAY",
     *       "status": "PUBLISHED",
     *       "externalId": "bm-67890",
     *       "externalUrl": "https://www.brightermonday.com/jobs/bm-67890"
     *     },
     *     {
     *       "partner": "LINKEDIN",
     *       "status": "PUBLISHED",
     *       "externalId": "li-abcde",
     *       "externalUrl": "https://www.linkedin.com/jobs/view/li-abcde"
     *     }
     *   ]
     * }
     */
    @PostMapping("/publish/{jobId}")
    public ResponseEntity<?> publishJob(
            @PathVariable String jobId,
            @RequestBody PublishRequest request,
            @AuthenticationPrincipal String userId) {
        try {
            log.info("Received publish request for job {} from user {} to countries {}",
                jobId, userId, request.getTargetCountries());

            // Vérifier l'ownership
            Optional<JobListing> jobOptional = jobListingRepository.findById(jobId);
            if (jobOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Job listing not found"));
            }

            JobListing job = jobOptional.get();
            if (!job.getCompanyId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("You do not have permission to publish this job"));
            }

            // Appeler le service d'orchestration
            PublishingReport report = jobBoardPublishingService.publishToJobBoards(
                jobId,
                request.getTargetCountries()
            );

            // Construire la réponse
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("jobId", report.getJobId());
            responseBody.put("timestamp", report.getTimestamp());
            responseBody.put("successCount", report.getSuccessCount());
            responseBody.put("failureCount", report.getFailureCount());
            responseBody.put("totalCount", report.getResults().size());
            responseBody.put("results", report.getResults().stream()
                .map(this::resultToMap)
                .toList());

            HttpStatus status = report.anySuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(responseBody);

        } catch (Exception e) {
            log.error("Unexpected error while publishing job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred while publishing the job"));
        }
    }

    /**
     * Dépublie une offre de tous les job boards.
     *
     * @param jobId ID de l'offre
     * @param userId utilisateur authentifié
     * @return rapport de dépublication
     *
     * Exemple requête:
     * DELETE /api/jobboards/unpublish/job-uuid-123
     *
     * Exemple réponse (200 OK):
     * {
     *   "jobId": "job-uuid-123",
     *   "timestamp": "2024-01-15T10:35:00",
     *   "successCount": 3,
     *   "failureCount": 0,
     *   "totalCount": 3,
     *   "results": [
     *     {
     *       "partner": "FRANCE_TRAVAIL",
     *       "status": "UNPUBLISHED"
     *     },
     *     ...
     *   ]
     * }
     */
    @DeleteMapping("/unpublish/{jobId}")
    public ResponseEntity<?> unpublishJob(
            @PathVariable String jobId,
            @AuthenticationPrincipal String userId) {
        try {
            log.info("Received unpublish request for job {} from user {}", jobId, userId);

            // Vérifier l'ownership
            Optional<JobListing> jobOptional = jobListingRepository.findById(jobId);
            if (jobOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Job listing not found"));
            }

            JobListing job = jobOptional.get();
            if (!job.getCompanyId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("You do not have permission to unpublish this job"));
            }

            // Appeler le service
            PublishingReport report = jobBoardPublishingService.unpublishFromJobBoards(jobId);

            // Construire la réponse
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("jobId", report.getJobId());
            responseBody.put("timestamp", report.getTimestamp());
            responseBody.put("successCount", report.getSuccessCount());
            responseBody.put("failureCount", report.getFailureCount());
            responseBody.put("totalCount", report.getResults().size());
            responseBody.put("results", report.getResults().stream()
                .map(this::resultToMap)
                .toList());

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            log.error("Unexpected error while unpublishing job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred while unpublishing the job"));
        }
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> resultToMap(JobBoardPublishResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("partner", result.getPartner());
        map.put("status", result.getStatus());
        if (result.getExternalId() != null) {
            map.put("externalId", result.getExternalId());
        }
        if (result.getExternalUrl() != null) {
            map.put("externalUrl", result.getExternalUrl());
        }
        if (result.getErrorMessage() != null) {
            map.put("errorMessage", result.getErrorMessage());
        }
        return map;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }

    // ==================== DTOs ====================

    public static class PublishRequest {
        private List<String> targetCountries;

        public PublishRequest() {}

        public PublishRequest(List<String> targetCountries) {
            this.targetCountries = targetCountries;
        }

        public List<String> getTargetCountries() {
            return targetCountries;
        }

        public void setTargetCountries(List<String> targetCountries) {
            this.targetCountries = targetCountries;
        }
    }
}
