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
 * Adaptateur pour LinkedIn job board.
 * Publie des offres via l'API LinkedIn Jobs.
 *
 * Documentation: https://learn.microsoft.com/en-us/linkedin/talent/job-postings-api
 *
 * TODO: Mapping exact des champs JobListing → LinkedIn API format
 *       À valider une fois les credentials LinkedIn obtenues.
 *       LinkedIn requiert OAuth2 + access token, pas Basic Auth.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedInAdapter implements JobBoardAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${linkedin.api.base-url:https://api.linkedin.com/v2}")
    private String baseUrl;

    @Value("${linkedin.api.access-token:}")
    private String accessToken;

    @Value("${linkedin.organization-id:}")
    private String organizationId;

    @Override
    public JobBoardPartner getPartner() {
        return JobBoardPartner.LINKEDIN;
    }

    @Override
    public JobBoardPublishResult publish(JobListing jobListing) {
        try {
            if (!isAvailable()) {
                return JobBoardPublishResult.failure(
                    JobBoardPartner.LINKEDIN,
                    "LinkedIn credentials not configured"
                );
            }

            Map<String, Object> requestBody = mapJobListingToLinkedIn(jobListing);
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Publishing job {} to LinkedIn", jobListing.getId());

            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/jobs",
                entity,
                String.class
            );

            if (response.getStatusCode().isError()) {
                String errorMsg = "LinkedIn API returned error status: " + response.getStatusCode();
                log.error(errorMsg);
                return JobBoardPublishResult.failure(JobBoardPartner.LINKEDIN, errorMsg);
            }

            String linkedInJobId = extractLinkedInJobId(response.getBody());
            String publicUrl = generatePublicUrl(linkedInJobId);

            log.info("Successfully published job {} to LinkedIn with ID: {}", jobListing.getId(), linkedInJobId);
            return JobBoardPublishResult.success(JobBoardPartner.LINKEDIN, linkedInJobId, publicUrl);

        } catch (RestClientException e) {
            String errorMsg = "Failed to communicate with LinkedIn API: " + e.getMessage();
            log.error(errorMsg, e);
            return JobBoardPublishResult.failure(JobBoardPartner.LINKEDIN, errorMsg);
        } catch (Exception e) {
            String errorMsg = "Unexpected error during LinkedIn publication: " + e.getMessage();
            log.error(errorMsg, e);
            return JobBoardPublishResult.failure(JobBoardPartner.LINKEDIN, errorMsg);
        }
    }

    @Override
    public boolean unpublish(String externalId) {
        try {
            if (externalId == null || externalId.isEmpty()) {
                log.warn("Cannot unpublish from LinkedIn: externalId is empty");
                return false;
            }

            HttpHeaders headers = buildHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            log.debug("Unpublishing from LinkedIn with ID: {}", externalId);

            ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl + "/jobs/" + externalId,
                HttpMethod.DELETE,
                entity,
                Void.class
            );

            if (response.getStatusCode().isError()) {
                log.error("LinkedIn API returned error status for delete: {}", response.getStatusCode());
                return false;
            }

            log.info("Successfully unpublished from LinkedIn");
            return true;

        } catch (Exception e) {
            log.error("Error while unpublishing from LinkedIn: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return accessToken != null && !accessToken.isEmpty() &&
               organizationId != null && !organizationId.isEmpty();
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> mapJobListingToLinkedIn(JobListing jobListing) {
        Map<String, Object> request = new HashMap<>();

        request.put("title", jobListing.getTitle());
        request.put("description", jobListing.getDescription());
        request.put("location", jobListing.getLocation());
        request.put("jobType", mapJobType(jobListing.getJobType()));

        // TODO: Valider format exact des salaires pour LinkedIn
        Map<String, Object> salary = new HashMap<>();
        salary.put("min", jobListing.getSalaryMin());
        salary.put("max", jobListing.getSalaryMax());
        salary.put("currency", "EUR");
        request.put("salary", salary);

        request.put("postedDate", LocalDateTime.now().toString());

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
            case "FREELANCE", "CONTRACTOR" -> "CONTRACT";
            default -> "FULL_TIME";
        };
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private String extractLinkedInJobId(String responseBody) throws Exception {
        // TODO: Adapter le parsing selon le format exact de réponse LinkedIn
        var json = objectMapper.readTree(responseBody);
        if (json.has("id")) {
            return json.get("id").asText();
        }
        if (json.has("jobId")) {
            return json.get("jobId").asText();
        }
        throw new RuntimeException("Could not extract LinkedIn job ID from response");
    }

    private String generatePublicUrl(String linkedInJobId) {
        return "https://www.linkedin.com/jobs/view/" + linkedInJobId;
    }
}
