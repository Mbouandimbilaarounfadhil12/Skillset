package com.skillset.infrastructure.integration.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillset.domain.entity.JobListing;
import com.skillset.domain.port.JobBoardAdapter;
import com.skillset.domain.value.JobBoardPartner;
import com.skillset.domain.value.JobBoardPublishResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptateur pour Jobartisan job board.
 * Publie des offres via l'API Jobartisan (plateforme africaine de recrutement).
 *
 * TODO: Accéder à la documentation API Jobartisan officielle pour :
 *       - URL exacte des endpoints
 *       - Format exact des champs requis
 *       - Authentification (API key, OAuth2, etc.)
 *       - Limite de taille des champs
 *       - Format des URL publiques générées
 *
 *       Implémentation actuelle basée sur conventions REST standard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobartisanAdapter implements JobBoardAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${jobartisan.api.base-url:https://api.jobartisan.io/v1}")
    private String baseUrl;

    @Value("${jobartisan.api.key:}")
    private String apiKey;

    @Override
    public JobBoardPartner getPartner() {
        return JobBoardPartner.JOBARTISAN;
    }

    @Override
    public JobBoardPublishResult publish(JobListing jobListing) {
        try {
            if (!isAvailable()) {
                return JobBoardPublishResult.failure(
                    JobBoardPartner.JOBARTISAN,
                    "Jobartisan API key not configured"
                );
            }

            Map<String, Object> requestBody = mapJobListingToJobartisan(jobListing);
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Publishing job {} to Jobartisan", jobListing.getId());

            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/jobs",
                entity,
                String.class
            );

            if (response.getStatusCode().isError()) {
                String errorMsg = "Jobartisan API returned error status: " + response.getStatusCode();
                log.error(errorMsg);
                return JobBoardPublishResult.failure(JobBoardPartner.JOBARTISAN, errorMsg);
            }

            String joJobId = extractJobartisanJobId(response.getBody());
            String publicUrl = generatePublicUrl(joJobId);

            log.info("Successfully published job {} to Jobartisan with ID: {}", jobListing.getId(), joJobId);
            return JobBoardPublishResult.success(JobBoardPartner.JOBARTISAN, joJobId, publicUrl);

        } catch (RestClientException e) {
            String errorMsg = "Failed to communicate with Jobartisan API: " + e.getMessage();
            log.error(errorMsg, e);
            return JobBoardPublishResult.failure(JobBoardPartner.JOBARTISAN, errorMsg);
        } catch (Exception e) {
            String errorMsg = "Unexpected error during Jobartisan publication: " + e.getMessage();
            log.error(errorMsg, e);
            return JobBoardPublishResult.failure(JobBoardPartner.JOBARTISAN, errorMsg);
        }
    }

    @Override
    public boolean unpublish(String externalId) {
        try {
            if (externalId == null || externalId.isEmpty()) {
                log.warn("Cannot unpublish from Jobartisan: externalId is empty");
                return false;
            }

            HttpHeaders headers = buildHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            log.debug("Unpublishing from Jobartisan with ID: {}", externalId);

            ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/jobs/" + externalId,
                HttpMethod.DELETE,
                entity,
                Void.class
            );

            if (response.getStatusCode().isError()) {
                log.error("Jobartisan API returned error status for delete: {}", response.getStatusCode());
                return false;
            }

            log.info("Successfully unpublished from Jobartisan");
            return true;

        } catch (Exception e) {
            log.error("Error while unpublishing from Jobartisan: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> mapJobListingToJobartisan(JobListing jobListing) {
        Map<String, Object> request = new HashMap<>();

        request.put("title", jobListing.getTitle());
        request.put("description", jobListing.getDescription());
        request.put("location", jobListing.getLocation());
        request.put("jobType", mapJobType(jobListing.getJobType()));

        // TODO: Valider format exact des salaires pour Jobartisan
        Map<String, Object> salary = new HashMap<>();
        salary.put("min", jobListing.getSalaryMin());
        salary.put("max", jobListing.getSalaryMax());
        request.put("salary", salary);

        request.put("publishedDate", LocalDateTime.now().toString());

        if (jobListing.getRequiredSkills() != null && !jobListing.getRequiredSkills().isEmpty()) {
            request.put("skills", jobListing.getRequiredSkills());
        }

        if (jobListing.getResponsibilities() != null && !jobListing.getResponsibilities().isEmpty()) {
            request.put("responsibilities", jobListing.getResponsibilities());
        }

        return request;
    }

    private String mapJobType(String skillsetJobType) {
        if (skillsetJobType == null) {
            return "FULL_TIME";
        }

        return switch (skillsetJobType.toUpperCase()) {
            case "CDI", "FULL_TIME" -> "FULL_TIME";
            case "CDD", "TEMPORARY" -> "TEMPORARY";
            case "STAGE", "INTERNSHIP" -> "INTERNSHIP";
            case "FREELANCE", "CONTRACTOR" -> "FREELANCE";
            default -> "FULL_TIME";
        };
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        // TODO: Adapter selon format d'authentification exact de Jobartisan
        return headers;
    }

    private String extractJobartisanJobId(String responseBody) throws Exception {
        // TODO: Adapter le parsing selon le format exact de réponse Jobartisan
        var json = objectMapper.readTree(responseBody);
        if (json.has("id")) {
            return json.get("id").asText();
        }
        if (json.has("jobId")) {
            return json.get("jobId").asText();
        }
        if (json.has("data") && json.get("data").has("id")) {
            return json.get("data").get("id").asText();
        }
        throw new RuntimeException("Could not extract Jobartisan job ID from response");
    }

    private String generatePublicUrl(String joJobId) {
        // TODO: Valider format exact de l'URL publique Jobartisan
        return "https://www.jobartisan.io/jobs/" + joJobId;
    }
}
