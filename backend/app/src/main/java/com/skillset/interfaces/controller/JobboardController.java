package com.skillset.interfaces.controller;

import com.skillset.domain.entity.JobListing;
import com.skillset.infrastructure.integration.FranceTravailService;
import com.skillset.infrastructure.persistence.JobListingRepository;
import com.skillset.infrastructure.util.FranceTravailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/jobboards/france-travail")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER')")
@Slf4j
public class JobboardController {

    private final FranceTravailService franceTravailService;
    private final JobListingRepository jobListingRepository;

    /**
     * Publishes a job listing to France Travail
     *
     * @param jobId the UUID of the JobListing to publish
     * @param userId the authenticated user ID (for ownership check)
     * @return PublishResponse with franceTravailId, publicUrl, and status
     */
    @PostMapping("/publish/{jobId}")
    public ResponseEntity<?> publishJob(
            @PathVariable String jobId,
            @AuthenticationPrincipal String userId) {
        try {
            log.info("Received publish request for job {} from user {}", jobId, userId);

            // Ownership check: verify userId owns the job
            Optional<JobListing> jobOptional = jobListingRepository.findById(jobId);
            if (jobOptional.isEmpty()) {
                log.warn("Job not found: {}", jobId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Job listing not found"));
            }

            JobListing job = jobOptional.get();
            if (!job.getCompanyId().equals(userId)) {
                log.warn("Unauthorized access attempt: user {} trying to publish job of company {}", userId, job.getCompanyId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("You do not have permission to publish this job"));
            }

            // Call service to publish job
            FranceTravailService.FranceTravailPublishResponse response = franceTravailService.publishJobListing(jobId);

            log.info("Successfully published job {} to France Travail with ID: {}", jobId, response.getFranceTravailId());

            // Build response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("franceTravailId", response.getFranceTravailId());
            responseBody.put("publicUrl", response.getPublicUrl());
            responseBody.put("status", "published");
            responseBody.put("message", "Job successfully published to France Travail");

            return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        } catch (FranceTravailException e) {
            log.error("FranceTravailException while publishing job {}: {}", jobId, e.getMessage(), e);
            return handleFranceTravailException(e);
        } catch (Exception e) {
            log.error("Unexpected error while publishing job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred while publishing the job"));
        }
    }

    /**
     * Unpublishes a job listing from France Travail
     *
     * @param jobId the UUID of the JobListing to unpublish
     * @param userId the authenticated user ID (for ownership check)
     * @return 204 No Content on success
     */
    @DeleteMapping("/unpublish/{jobId}")
    public ResponseEntity<?> unpublishJob(
            @PathVariable String jobId,
            @AuthenticationPrincipal String userId) {
        try {
            log.info("Received unpublish request for job {} from user {}", jobId, userId);

            // Ownership check: verify userId owns the job
            Optional<JobListing> jobOptional = jobListingRepository.findById(jobId);
            if (jobOptional.isEmpty()) {
                log.warn("Job not found: {}", jobId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Job listing not found"));
            }

            JobListing job = jobOptional.get();
            if (!job.getCompanyId().equals(userId)) {
                log.warn("Unauthorized access attempt: user {} trying to unpublish job of company {}", userId, job.getCompanyId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("You do not have permission to unpublish this job"));
            }

            // Call service to unpublish job
            boolean success = franceTravailService.unpublishJobListing(jobId);

            if (!success) {
                log.warn("Failed to unpublish job {} from France Travail", jobId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Job was not published or unpublish failed"));
            }

            log.info("Successfully unpublished job {} from France Travail", jobId);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("Unexpected error while unpublishing job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred while unpublishing the job"));
        }
    }

    /**
     * Gets the status of a job listing on France Travail
     *
     * @param jobId the UUID of the JobListing
     * @param userId the authenticated user ID (for ownership check)
     * @return JobListing with France Travail details
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getJobStatus(
            @PathVariable String jobId,
            @AuthenticationPrincipal String userId) {
        try {
            log.info("Received status request for job {} from user {}", jobId, userId);

            // Ownership check: verify userId owns the job
            Optional<JobListing> jobOptional = jobListingRepository.findById(jobId);
            if (jobOptional.isEmpty()) {
                log.warn("Job not found: {}", jobId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Job listing not found"));
            }

            JobListing job = jobOptional.get();
            if (!job.getCompanyId().equals(userId)) {
                log.warn("Unauthorized access attempt: user {} trying to get status of job of company {}", userId, job.getCompanyId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("You do not have permission to view this job"));
            }

            // Call service to sync status
            JobListing updatedJob = franceTravailService.syncJobStatus(jobId);

            log.info("Retrieved status for job {} from France Travail", jobId);

            // Build response with France Travail details
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("jobId", updatedJob.getId());
            responseBody.put("title", updatedJob.getTitle());
            responseBody.put("franceTravailId", updatedJob.getFranceTravailId());
            responseBody.put("publishedOnFranceTravail", updatedJob.getPublishedOnFranceTravail());
            responseBody.put("franceTravailUrl", updatedJob.getFranceTravailUrl());
            responseBody.put("postedAt", updatedJob.getPostedAt());
            responseBody.put("status", updatedJob.getStatus());

            return ResponseEntity.ok(responseBody);

        } catch (FranceTravailException e) {
            log.error("FranceTravailException while getting job status {}: {}", jobId, e.getMessage(), e);
            return handleFranceTravailException(e);
        } catch (Exception e) {
            log.error("Unexpected error while getting job status {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred while retrieving job status"));
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Handles FranceTravailException and returns appropriate HTTP status
     */
    private ResponseEntity<?> handleFranceTravailException(FranceTravailException e) {
        int httpStatus = e.getHttpStatus();

        return switch (httpStatus) {
            case 401 -> {
                log.error("Authentication failed with France Travail API");
                yield ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Authentication failed with France Travail API"));
            }
            case 429 -> {
                log.error("Rate limit exceeded on France Travail API");
                yield ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(createErrorResponse("Too many requests to France Travail API. Please try again later."));
            }
            case 400 -> {
                log.error("Bad request to France Travail API: {}", e.getMessage());
                yield ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Invalid request to France Travail API: " + e.getMessage()));
            }
            case 404 -> {
                log.error("Resource not found on France Travail API: {}", e.getMessage());
                yield ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Resource not found: " + e.getMessage()));
            }
            default -> {
                log.error("France Travail API error ({}): {}", httpStatus, e.getMessage());
                yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error communicating with France Travail API"));
            }
        };
    }

    /**
     * Creates a standard error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }
}
