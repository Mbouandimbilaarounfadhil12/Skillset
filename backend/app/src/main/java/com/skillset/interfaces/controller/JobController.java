package com.skillset.interfaces.controller;

import com.skillset.application.dto.JobListingDTO;
import com.skillset.application.dto.ScoredCandidateDTO;
import com.skillset.application.dto.ScoredJobDTO;
import com.skillset.application.service.JobService;
import com.skillset.domain.entity.JobListing;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<JobListing> createJob(@AuthenticationPrincipal String userId,
                                                @RequestBody JobListing jobListing) {
        JobListing createdJob = jobService.createJob(userId, jobListing);
        return new ResponseEntity<>(createdJob, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<JobListingDTO>> getAllOpenJobs(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) Integer salaryMin,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) String sort) {
        List<JobListingDTO> jobs = jobService.searchJobs(userId, q, location, type, sector, salaryMin, remote, sort);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/search")
    public ResponseEntity<List<JobListingDTO>> searchJobs(@RequestParam String location) {
        List<JobListingDTO> jobs = jobService.getJobsByLocation(location);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasRole('ADMIN') or #companyId == authentication.principal")
    public ResponseEntity<List<JobListingDTO>> getCompanyJobs(@AuthenticationPrincipal String currentUserId,
                                                                @PathVariable String companyId) {
        List<JobListingDTO> jobs = jobService.getCompanyJobs(currentUserId, companyId);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/suggested")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<List<ScoredJobDTO>> getSuggestedJobs(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(jobService.getSuggestedJobs(userId));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobListing> getJobById(@PathVariable String jobId) {
        var job = jobService.getJobById(jobId);
        if (job.isPresent()) {
            return ResponseEntity.ok(job.get());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{jobId}/suggested-candidates")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<List<ScoredCandidateDTO>> getSuggestedCandidates(
            @AuthenticationPrincipal String userId, @PathVariable String jobId) {
        return ResponseEntity.ok(jobService.getSuggestedCandidates(userId, jobId));
    }

    @PutMapping("/{jobId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<JobListing> updateJob(@AuthenticationPrincipal String userId,
                                                @PathVariable String jobId,
                                                @RequestBody JobListing jobDetails) {
        JobListing updatedJob = jobService.updateJob(userId, jobId, jobDetails);
        if (updatedJob != null) {
            return ResponseEntity.ok(updatedJob);
        }
        return ResponseEntity.notFound().build();
    }
}
