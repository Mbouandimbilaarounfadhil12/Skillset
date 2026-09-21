package com.skillset.interfaces.controller;

import com.skillset.application.dto.JobListingDTO;
import com.skillset.application.dto.PublicCareersDto;
import com.skillset.domain.entity.EmployerProfile;
import com.skillset.domain.entity.JobListing;
import com.skillset.domain.entity.JobStatus;
import com.skillset.infrastructure.persistence.EmployerProfileRepository;
import com.skillset.infrastructure.persistence.JobListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/public/careers")
@RequiredArgsConstructor
public class PublicCareersController {

    private final EmployerProfileRepository employerProfileRepository;
    private final JobListingRepository      jobListingRepository;

    @GetMapping("/{slug}")
    public ResponseEntity<PublicCareersDto> getCareerPage(@PathVariable String slug) {
        return employerProfileRepository.findByCompanySlug(slug)
            .map(profile -> {
                List<JobListingDTO> jobs = jobListingRepository
                    .findByCompanyIdAndStatus(profile.getUserId(), JobStatus.OPEN)
                    .stream()
                    .map(job -> toDto(job, profile))
                    .toList();
                return ResponseEntity.ok(new PublicCareersDto(
                    profile.getCompanySlug(),
                    profile.getCompanyName(),
                    profile.getIndustry(),
                    profile.getCompanySize(),
                    profile.getCompanyCity(),
                    profile.getCompanyCountry(),
                    profile.getCompanyWebsite(),
                    profile.getCompanyLinkedIn(),
                    jobs
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private JobListingDTO toDto(JobListing j, EmployerProfile profile) {
        List<String> skills = j.getRequiredSkills() == null || j.getRequiredSkills().isBlank()
            ? Collections.emptyList()
            : Arrays.stream(j.getRequiredSkills().split(",")).map(String::trim).toList();
        int postedDaysAgo = j.getPostedAt() != null
            ? (int) Duration.between(j.getPostedAt(), LocalDateTime.now()).toDays()
            : 0;
        String currency = "France".equalsIgnoreCase(profile.getCompanyCountry()) ? "EUR" : "FCFA";

        return new JobListingDTO(
            j.getId(), j.getTitle(), j.getDescription(), j.getCompanyId(),
            j.getLocation(), j.getJobType(), j.getSalaryMin(), j.getSalaryMax(),
            j.getRequiredSkills(), j.getResponsibilities(), j.getStatus().name(),
            profile.getCompanyName(), profile.getIndustry(), currency, skills,
            false, postedDaysAgo, 0, postedDaysAgo <= 2, null
        );
    }
}
