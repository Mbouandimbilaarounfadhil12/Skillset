package com.skillset.infrastructure.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillset.domain.entity.JobListing;
import com.skillset.infrastructure.persistence.JobListingRepository;
import com.skillset.infrastructure.util.FranceTravailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FranceTravailService {

    private final RestTemplate restTemplate;
    private final JobListingRepository jobListingRepository;
    private final ObjectMapper objectMapper;

    @Value("${france-travail.api.base-url:https://api.francetravail.io/partenaire/offresdemploi/v2}")
    private String baseUrl;

    @Value("${france-travail.api.client-id:}")
    private String clientId;

    @Value("${france-travail.api.client-secret:}")
    private String clientSecret;

    @Value("${integration.api.timeout:30000}")
    private int timeout;

    /**
     * Publishes a job listing to France Travail API
     *
     * @param jobId the UUID of the JobListing to publish
     * @return FranceTravailPublishResponse with franceTravailId and publicUrl
     * @throws FranceTravailException if the API call fails
     */
    public FranceTravailPublishResponse publishJobListing(String jobId) {
        try {
            // Fetch job listing from repository
            Optional<JobListing> jobOptional = jobListingRepository.findById(jobId);
            if (jobOptional.isEmpty()) {
                log.warn("Job listing not found for ID: {}", jobId);
                throw new FranceTravailException("Job listing not found with ID: " + jobId, 404);
            }

            JobListing jobListing = jobOptional.get();
            if (jobListing.getPublishedOnFranceTravail() != null && jobListing.getPublishedOnFranceTravail()) {
                log.info("Job {} already published to France Travail", jobId);
                return new FranceTravailPublishResponse(jobListing.getFranceTravailId(), jobListing.getFranceTravailUrl());
            }

            // Map JobListing to France Travail API format
            Map<String, Object> requestBody = mapJobListingToFranceTravail(jobListing);

            // Build headers
            HttpHeaders headers = buildHeaders();

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Publishing job {} to France Travail with URL: {}/offres", jobId, baseUrl);

            // Call France Travail API
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/offres",
                entity,
                String.class
            );

            if (response.getStatusCode().isError()) {
                log.error("France Travail API returned error status: {}", response.getStatusCode());
                throw new FranceTravailException(
                    "Failed to publish to France Travail: " + response.getStatusCode(),
                    response.getStatusCode().value()
                );
            }

            // Parse response to extract idFt and sourceLibelle
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            String franceTravailId = extractFranceTravailId(responseNode);
            String publicUrl = generatePublicUrl(franceTravailId);

            log.info("Successfully published job {} to France Travail with ID: {}", jobId, franceTravailId);

            // Update JobListing with France Travail details
            jobListing.setFranceTravailId(franceTravailId);
            jobListing.setPublishedOnFranceTravail(true);
            jobListing.setFranceTravailUrl(publicUrl);
            jobListingRepository.save(jobListing);

            log.debug("Updated JobListing {} with France Travail details", jobId);

            return new FranceTravailPublishResponse(franceTravailId, publicUrl);

        } catch (FranceTravailException e) {
            log.error("FranceTravailException while publishing job {}: {}", jobId, e.getMessage(), e);
            throw e;
        } catch (RestClientException e) {
            log.error("RestClientException while publishing job {}: {}", jobId, e.getMessage(), e);
            throw new FranceTravailException("Failed to communicate with France Travail API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while publishing job {} to France Travail: {}", jobId, e.getMessage(), e);
            throw new FranceTravailException("Unexpected error during France Travail publication: " + e.getMessage(), e);
        }
    }

    /**
     * Unpublishes a job listing from France Travail API
     *
     * @param jobId the UUID of the JobListing to unpublish
     * @return true if unpublish was successful, false otherwise
     */
    public boolean unpublishJobListing(String jobId) {
        try {
            // Fetch job listing
            Optional<JobListing> jobOptional = jobListingRepository.findById(jobId);
            if (jobOptional.isEmpty()) {
                log.warn("Job listing not found for ID: {}", jobId);
                return false;
            }

            JobListing jobListing = jobOptional.get();

            // Check if published on France Travail
            if (jobListing.getFranceTravailId() == null || jobListing.getFranceTravailId().isEmpty()) {
                log.info("Job {} has no France Travail ID, skipping unpublish", jobId);
                return false;
            }

            String franceTravailId = jobListing.getFranceTravailId();

            // Build headers
            HttpHeaders headers = buildHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            log.debug("Unpublishing job {} from France Travail with ID: {}", jobId, franceTravailId);

            // Call DELETE endpoint
            ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/offres/" + franceTravailId,
                HttpMethod.DELETE,
                entity,
                Void.class
            );

            if (response.getStatusCode().isError()) {
                log.error("France Travail API returned error status for delete: {}", response.getStatusCode());
                return false;
            }

            log.info("Successfully unpublished job {} from France Travail", jobId);

            // Update JobListing
            jobListing.setPublishedOnFranceTravail(false);
            jobListing.setFranceTravailId(null);
            jobListing.setFranceTravailUrl(null);
            jobListingRepository.save(jobListing);

            return true;

        } catch (RestClientException e) {
            log.error("RestClientException while unpublishing job {}: {}", jobId, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while unpublishing job {} from France Travail: {}", jobId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Syncs the status of a job listing with France Travail API
     *
     * @param jobId the UUID of the JobListing to sync
     * @return the updated JobListing entity
     */
    public JobListing syncJobStatus(String jobId) {
        try {
            // Fetch job listing
            Optional<JobListing> jobOptional = jobListingRepository.findById(jobId);
            if (jobOptional.isEmpty()) {
                log.warn("Job listing not found for ID: {}", jobId);
                throw new FranceTravailException("Job listing not found with ID: " + jobId, 404);
            }

            JobListing jobListing = jobOptional.get();

            // Check if published on France Travail
            if (jobListing.getFranceTravailId() == null || jobListing.getFranceTravailId().isEmpty()) {
                log.debug("Job {} has no France Travail ID, returning unchanged", jobId);
                return jobListing;
            }

            String franceTravailId = jobListing.getFranceTravailId();

            // Build headers
            HttpHeaders headers = buildHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            log.debug("Syncing status for job {} with France Travail ID: {}", jobId, franceTravailId);

            // Call GET endpoint
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/offres/" + franceTravailId,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode().isError()) {
                log.error("France Travail API returned error status for get: {}", response.getStatusCode());
                return jobListing;
            }

            // Parse response and check status field
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            String ftStatus = extractFranceTravailStatus(responseNode);

            log.debug("Retrieved status from France Travail for job {}: {}", jobId, ftStatus);

            // Update local status if different (optional: map ftStatus to JobStatus)
            // For now, we just log it
            log.info("Job {} status from France Travail: {}", jobId, ftStatus);

            jobListingRepository.save(jobListing);

            return jobListing;

        } catch (FranceTravailException e) {
            log.error("FranceTravailException while syncing job {}: {}", jobId, e.getMessage(), e);
            throw e;
        } catch (RestClientException e) {
            log.error("RestClientException while syncing job {}: {}", jobId, e.getMessage(), e);
            // Return unchanged JobListing on error
            return jobListingRepository.findById(jobId).orElseThrow(() ->
                new FranceTravailException("Job not found after sync failure", 404)
            );
        } catch (Exception e) {
            log.error("Unexpected error while syncing job {}: {}", jobId, e.getMessage(), e);
            return jobListingRepository.findById(jobId).orElseThrow(() ->
                new FranceTravailException("Job not found after sync failure", 500)
            );
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Maps a JobListing to France Travail API format
     */
    private Map<String, Object> mapJobListingToFranceTravail(JobListing jobListing) {
        Map<String, Object> request = new HashMap<>();

        // Required fields
        request.put("intitule", jobListing.getTitle());

        // Description (truncate to 2000 chars max)
        String description = jobListing.getDescription() != null ?
            jobListing.getDescription() : "";
        if (description.length() > 2000) {
            description = description.substring(0, 2000);
        }
        request.put("description", description);

        // Type de contrat (CDI, CDD, STAGE, FREELANCE, etc.)
        request.put("typeContrat", mapJobType(jobListing.getJobType()));

        // Salary
        Map<String, String> salaire = new HashMap<>();
        String salaryRange = jobListing.getSalaryMin() + "-" + jobListing.getSalaryMax() + " €";
        salaire.put("libelle", salaryRange);
        request.put("salaire", salaire);

        // Location
        Map<String, String> lieuTravail = new HashMap<>();
        lieuTravail.put("libelle", jobListing.getLocation());
        request.put("lieuTravail", lieuTravail);

        // Skills (parse as array)
        if (jobListing.getRequiredSkills() != null && !jobListing.getRequiredSkills().isEmpty()) {
            String[] skillsArray = parseSkillsArray(jobListing.getRequiredSkills());
            request.put("competences", skillsArray);
        }

        // Publication date
        request.put("datePublication", formatDateTimeForAPI(LocalDateTime.now()));

        // Additional optional fields
        if (jobListing.getResponsibilities() != null && !jobListing.getResponsibilities().isEmpty()) {
            request.put("missions", jobListing.getResponsibilities());
        }

        return request;
    }

    /**
     * Maps SkillSet job type to France Travail API format
     */
    private String mapJobType(String skillsetJobType) {
        if (skillsetJobType == null) {
            return "CDI";
        }

        return switch (skillsetJobType.toUpperCase()) {
            case "CDI" -> "CDI";
            case "CDD" -> "CDD";
            case "STAGE", "INTERNSHIP" -> "STAGE";
            case "FREELANCE", "CONTRACTOR" -> "FREELANCE";
            case "TEMPORARY" -> "TEMPS";
            default -> "CDI";
        };
    }

    /**
     * Parses skills string to array
     */
    private String[] parseSkillsArray(String skillsString) {
        if (skillsString == null || skillsString.isEmpty()) {
            return new String[0];
        }

        // Try to parse as JSON array first
        try {
            JsonNode node = objectMapper.readTree(skillsString);
            if (node.isArray()) {
                String[] skills = new String[node.size()];
                for (int i = 0; i < node.size(); i++) {
                    skills[i] = node.get(i).asText();
                }
                return skills;
            }
        } catch (Exception e) {
            log.debug("Could not parse skills as JSON, treating as comma-separated: {}", skillsString);
        }

        // Fall back to comma-separated parsing
        return skillsString.split(",\\s*");
    }

    /**
     * Builds HTTP headers for France Travail API calls
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Add authentication if credentials are available
        if (clientId != null && !clientId.isEmpty() && clientSecret != null && !clientSecret.isEmpty()) {
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            headers.set("Authorization", "Basic " + encodedCredentials);
        }

        headers.set("Accept", "application/json");
        headers.set("User-Agent", "SkillSet/1.0");

        return headers;
    }

    /**
     * Extracts France Travail ID from API response
     */
    private String extractFranceTravailId(JsonNode responseNode) {
        // Try common field names: id, idFt, uuid
        if (responseNode.has("id")) {
            return responseNode.get("id").asText();
        }
        if (responseNode.has("idFt")) {
            return responseNode.get("idFt").asText();
        }
        if (responseNode.has("uuid")) {
            return responseNode.get("uuid").asText();
        }

        log.warn("Could not extract France Travail ID from response: {}", responseNode);
        throw new FranceTravailException("Invalid response from France Travail API: missing ID field");
    }

    /**
     * Extracts status from France Travail API response
     */
    private String extractFranceTravailStatus(JsonNode responseNode) {
        if (responseNode.has("status")) {
            return responseNode.get("status").asText();
        }
        if (responseNode.has("etat")) {
            return responseNode.get("etat").asText();
        }
        return "UNKNOWN";
    }

    /**
     * Generates public URL for the job listing on France Travail
     */
    private String generatePublicUrl(String franceTravailId) {
        return "https://www.francetravail.fr/offres/" + franceTravailId;
    }

    /**
     * Formats LocalDateTime for API consumption
     */
    private String formatDateTimeForAPI(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return dateTime.format(formatter);
    }

    /**
     * DTO for France Travail publish response
     */
    public static class FranceTravailPublishResponse {
        public final String franceTravailId;
        public final String publicUrl;

        public FranceTravailPublishResponse(String franceTravailId, String publicUrl) {
            this.franceTravailId = franceTravailId;
            this.publicUrl = publicUrl;
        }

        public String getFranceTravailId() {
            return franceTravailId;
        }

        public String getPublicUrl() {
            return publicUrl;
        }
    }
}
