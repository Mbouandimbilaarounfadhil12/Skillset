package com.skillset.interfaces.controller;

import com.skillset.domain.entity.JobListing;
import com.skillset.domain.entity.JobStatus;
import com.skillset.infrastructure.persistence.JobListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Incoming webhook: POST /api/public/webhooks/import-jobs
 * Secured by X-Webhook-Secret header matching webhook.import.secret property.
 * Payload: array of job objects — title, description, companyId, location,
 *   jobType, salaryMin, salaryMax, requiredSkills, responsibilities.
 */
@RestController
@RequestMapping("/api/public/webhooks")
@RequiredArgsConstructor
public class WebhookImportController {

    private final JobListingRepository jobListingRepository;

    @Value("${webhook.import.secret}")
    private String webhookSecret;

    @PostMapping("/import-jobs")
    public ResponseEntity<Map<String, Object>> importJobs(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String incomingSecret,
            @RequestBody List<Map<String, String>> payload) {

        if (!webhookSecret.equals(incomingSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Secret invalide ou manquant"));
        }

        if (payload == null || payload.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Payload vide"));
        }

        int created = 0;
        for (Map<String, String> item : payload) {
            String title     = item.get("title");
            String companyId = item.get("companyId");
            String location  = item.get("location");
            String jobType   = item.get("jobType");
            if (title == null || companyId == null || location == null || jobType == null) continue;

            JobListing job = new JobListing();
            job.setTitle(title);
            job.setDescription(item.getOrDefault("description", ""));
            job.setCompanyId(companyId);
            job.setLocation(location);
            job.setJobType(jobType);
            job.setSalaryMin(item.getOrDefault("salaryMin", ""));
            job.setSalaryMax(item.getOrDefault("salaryMax", ""));
            job.setRequiredSkills(item.getOrDefault("requiredSkills", ""));
            job.setResponsibilities(item.getOrDefault("responsibilities", ""));
            job.setStatus(JobStatus.OPEN);
            job.setPostedAt(LocalDateTime.now());
            job.setCreatedAt(LocalDateTime.now());
            jobListingRepository.save(job);
            created++;
        }

        return ResponseEntity.ok(Map.of("imported", created));
    }
}
