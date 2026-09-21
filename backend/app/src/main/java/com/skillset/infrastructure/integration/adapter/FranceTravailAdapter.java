package com.skillset.infrastructure.integration.adapter;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Adaptateur pour France Travail job board.
 * Implémente le contrat JobBoardAdapter pour publier/dépublier des offres
 * sur l'API France Travail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FranceTravailAdapter implements JobBoardAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${france-travail.api.base-url:https://api.francetravail.io/partenaire/offresdemploi/v2}")
    private String baseUrl;

    @Value("${france-travail.api.client-id:}")
    private String clientId;

    @Value("${france-travail.api.client-secret:}")
    private String clientSecret;

    @Override
    public JobBoardPartner getPartner() {
        return JobBoardPartner.FRANCE_TRAVAIL;
    }

    @Override
    public JobBoardPublishResult publish(JobListing jobListing) {
        try {
            // Map JobListing to France Travail API format
            Map<String, Object> requestBody = mapJobListingToFranceTravail(jobListing);

            // Build headers
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Publishing job {} to France Travail with URL: {}/offres", jobListing.getId(), baseUrl);

            // Call France Travail API
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/offres",
                entity,
                String.class
            );

            if (response.getStatusCode().isError()) {
                String errorMsg = "France Travail API returned error status: " + response.getStatusCode();
                log.error(errorMsg);
                return JobBoardPublishResult.failure(JobBoardPartner.FRANCE_TRAVAIL, errorMsg);
            }

            // Parse response to extract ID and generate URL
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            String franceTravailId = extractFranceTravailId(responseNode);
            String publicUrl = generatePublicUrl(franceTravailId);

            log.info("Successfully published job {} to France Travail with ID: {}", jobListing.getId(), franceTravailId);
            return JobBoardPublishResult.success(JobBoardPartner.FRANCE_TRAVAIL, franceTravailId, publicUrl);

        } catch (RestClientException e) {
            String errorMsg = "Failed to communicate with France Travail API: " + e.getMessage();
            log.error(errorMsg, e);
            return JobBoardPublishResult.failure(JobBoardPartner.FRANCE_TRAVAIL, errorMsg);
        } catch (Exception e) {
            String errorMsg = "Unexpected error during France Travail publication: " + e.getMessage();
            log.error(errorMsg, e);
            return JobBoardPublishResult.failure(JobBoardPartner.FRANCE_TRAVAIL, errorMsg);
        }
    }

    @Override
    public boolean unpublish(String externalId) {
        try {
            if (externalId == null || externalId.isEmpty()) {
                log.warn("Cannot unpublish: externalId is empty");
                return false;
            }

            HttpHeaders headers = buildHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            log.debug("Unpublishing from France Travail with ID: {}", externalId);

            ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/offres/" + externalId,
                HttpMethod.DELETE,
                entity,
                Void.class
            );

            if (response.getStatusCode().isError()) {
                log.error("France Travail API returned error status for delete: {}", response.getStatusCode());
                return false;
            }

            log.info("Successfully unpublished from France Travail");
            return true;

        } catch (RestClientException e) {
            log.error("RestClientException while unpublishing: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while unpublishing: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return clientId != null && !clientId.isEmpty() &&
               clientSecret != null && !clientSecret.isEmpty();
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> mapJobListingToFranceTravail(JobListing jobListing) {
        Map<String, Object> request = new HashMap<>();

        request.put("intitule", jobListing.getTitle());

        String description = jobListing.getDescription() != null ?
            jobListing.getDescription() : "";
        if (description.length() > 2000) {
            description = description.substring(0, 2000);
        }
        request.put("description", description);

        request.put("typeContrat", mapJobType(jobListing.getJobType()));

        Map<String, String> salaire = new HashMap<>();
        String salaryRange = jobListing.getSalaryMin() + "-" + jobListing.getSalaryMax() + " €";
        salaire.put("libelle", salaryRange);
        request.put("salaire", salaire);

        Map<String, String> lieuTravail = new HashMap<>();
        lieuTravail.put("libelle", jobListing.getLocation());
        request.put("lieuTravail", lieuTravail);

        if (jobListing.getRequiredSkills() != null && !jobListing.getRequiredSkills().isEmpty()) {
            String[] skillsArray = parseSkillsArray(jobListing.getRequiredSkills());
            request.put("competences", skillsArray);
        }

        request.put("datePublication", formatDateTimeForAPI(LocalDateTime.now()));

        if (jobListing.getResponsibilities() != null && !jobListing.getResponsibilities().isEmpty()) {
            request.put("missions", jobListing.getResponsibilities());
        }

        return request;
    }

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

    private String[] parseSkillsArray(String skillsString) {
        if (skillsString == null || skillsString.isEmpty()) {
            return new String[0];
        }

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

        return skillsString.split(",\\s*");
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (clientId != null && !clientId.isEmpty() && clientSecret != null && !clientSecret.isEmpty()) {
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            headers.set("Authorization", "Basic " + encodedCredentials);
        }

        headers.set("Accept", "application/json");
        headers.set("User-Agent", "SkillSet/1.0");

        return headers;
    }

    private String extractFranceTravailId(JsonNode responseNode) {
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
        throw new RuntimeException("Invalid response from France Travail API: missing ID field");
    }

    private String generatePublicUrl(String franceTravailId) {
        return "https://www.francetravail.fr/offres/" + franceTravailId;
    }

    private String formatDateTimeForAPI(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return dateTime.format(formatter);
    }
}
