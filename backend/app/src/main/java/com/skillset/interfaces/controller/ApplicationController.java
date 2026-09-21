package com.skillset.interfaces.controller;

import com.skillset.application.dto.ApplicationDTO;
import com.skillset.application.service.ApplicationService;
import com.skillset.domain.entity.Application;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Application> submitApplication(
            @AuthenticationPrincipal String userId,
            @RequestPart("jobListingId") String jobListingId,
            @RequestPart(value = "coverLetter", required = false) String coverLetter,
            @RequestPart(value = "cv", required = false) MultipartFile cvFile) {

        Application app = applicationService.submitApplication(userId, jobListingId, coverLetter, cvFile);
        return new ResponseEntity<>(app, HttpStatus.CREATED);
    }

    @GetMapping("/employer")
    @PreAuthorize("hasRole('EMPLOYER') or hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationDTO>> getEmployerApplications(
            @AuthenticationPrincipal String currentUserId) {
        return ResponseEntity.ok(applicationService.getEmployerApplications(currentUserId));
    }

    @GetMapping("/candidate/{jobSeekerId}")
    @PreAuthorize("hasRole('ADMIN') or #jobSeekerId == authentication.principal")
    public ResponseEntity<List<ApplicationDTO>> getCandidateApplications(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String jobSeekerId) {
        return ResponseEntity.ok(applicationService.getCandidateApplications(currentUserId, jobSeekerId));
    }

    @GetMapping("/job/{jobListingId}")
    @PreAuthorize("hasRole('EMPLOYER') or hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationDTO>> getJobApplications(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String jobListingId) {
        return ResponseEntity.ok(applicationService.getJobApplications(currentUserId, jobListingId));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<Application> getApplicationById(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String applicationId) {
        return applicationService.getApplicationById(currentUserId, applicationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<ApplicationDTO> updateStatus(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String applicationId,
            @RequestParam String status) {
        return ResponseEntity.ok(applicationService.updateStatus(currentUserId, applicationId, status));
    }
}
