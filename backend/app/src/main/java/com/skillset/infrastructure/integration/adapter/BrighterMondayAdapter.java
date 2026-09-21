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
 * Adaptateur pour BrighterMonday job board.
 * Publie des offres via l'API BrighterMonday (plateforme de recrutement panafricaine).
 *
 * BrighterMonday couvre : Kenya, Uganda, Nigeria, Ghana, Cameroon, Senegal, etc.
 *
 * TODO: Accéder à la documentation API BrighterMonday officielle pour :
 *       - URL exacte des endpoints
 *       - Format exact des champs requis
 *       - Authentification (API key, Bearer token, etc.)
 *       - Limite de taille des champs
 *       - Format des URL publiques générées
 *
 *       Implémentation actuelle basée sur conventions REST standard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrighterMondayAdapter implements JobBoardAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${brightermonday.api.base-url:https://api.brightermonday.com/v1}")
    private String baseUrl;

    @Value("${brightermonday.api.key:}")
    private String apiKey;

    @Override
    public JobBoardPartner getPartner() {
        return JobBoardPartner.BRIGHTERMONDAY;
    }

    @Override
    public JobBoardPublishResult publish(JobListing jobListing) {
        try {
            if (!isAvailable()) {
                return JobBoardPublishResult.failure(
                    JobBoardPartner.BRIGHTERMONDAY,
                    "BrighterMonday API key not configured"
                );
            }

            Map<String, Object> requestBody = mapJobListingToBrighterMonday(jobListing);
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Publishing job {} to BrighterMonday", jobListing.getId());

            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/jobs",
                entity,
                String.class
            );

            if (response.getStatusCode().isError()) {
                String errorMsg = "BrighterMonday API returned error status: " + response.getStatusCode();
                log.error(errorMsg);
                return JobBoardPublishResult.failure(JobBoardPartner.BRIGHTERMONDAY, errorMsg);
            }

            String bmJobId = extractBrighterMondayJobId(response.getBody());
            String publicUrl = generatePublicUrl(bmJobId);

            log.info("Successfully published job {} to BrighterMonday with ID: {}", jobListing.getId(), bmJobId);
            return JobBoardPublishResult.success(JobBoardPartner.BRIGHTERMONDAY, bmJobId, publicUrl);

        } catch (RestClientException e) {
            String errorMsg = "Failed to communicate with BrighterMonday API: " + e.getMessage();
            log.error(errorMsg, e);
            return JobBoardPublishResult.failure(JobBoardPartner.BRIGHTERMONDAY, errorMsg);
        } catch (Exception e) {
            String errorMsg = "Unexpected error during BrighterMonday publication: " + e.getMessage();
            log.error(errorMsg, e);
            return JobBoardPublishResult.failure(JobBoardPartner.BRIGHTERMONDAY, errorMsg);
        }
    }

    @Override
    public boolean unpublish(String externalId) {
        try {
            if (externalId == null || externalId.isEmpty()) {
                log.warn("Cannot unpublish from BrighterMonday: externalId is empty");
                return false;
            }

            HttpHeaders headers = buildHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            log.debug("Unpublishing from BrighterMonday with ID: {}", externalId);

            ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/jobs/" + externalId,
                HttpMethod.DELETE,
                entity,
                Void.class
            );

            if (response.getStatusCode().isError()) {
                log.error("BrighterMonday API returned error status for delete: {}", response.getStatusCode());
                return false;
            }

            log.info("Successfully unpublished from BrighterMonday");
            return true;

        } catch (Exception e) {
            log.error("Error while unpublishing from BrighterMonday: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> mapJobListingToBrighterMonday(JobListing jobListing) {
        Map<String, Object> request = new HashMap<>();

        request.put("title", jobListing.getTitle());
        request.put("description", jobListing.getDescription());
        request.put("location", jobListing.getLocation());
        request.put("jobType", mapJobType(jobListing.getJobType()));

        // TODO: Valider format exact des salaires pour BrighterMonday
        Map<String, Object> salary = new HashMap<>();
        salary.put("minimum", jobListing.getSalaryMin());
        salary.put("maximum", jobListing.getSalaryMax());
        salary.put("currency", "USD"); // TODO: À adapter selon la région cible
        request.put("salary", salary);

        request.put("postDate", LocalDateTime.now().toString());
        request.put("postedAt", LocalDateTime.now().toString());

        if (jobListing.getRequiredSkills() != null && !jobListing.getRequiredSkills().isEmpty()) {
            request.put("requiredSkills", jobListing.getRequiredSkills());
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
        // TODO: Adapter selon format d'authentification exact de BrighterMonday
        return headers;
    }

    private String extractBrighterMondayJobId(String responseBody) throws Exception {
        // TODO: Adapter le parsing selon le format exact de réponse BrighterMonday
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
        throw new RuntimeException("Could not extract BrighterMonday job ID from response");
    }

    private String generatePublicUrl(String bmJobId) {
        // TODO: Valider format exact de l'URL publique BrighterMonday
        return "https://www.brightermonday.com/jobs/" + bmJobId;
    }
}
