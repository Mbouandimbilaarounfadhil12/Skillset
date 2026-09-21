package com.skillset.interfaces.controller;

import com.skillset.application.dto.AdminStatsDto;
import com.skillset.application.dto.RecruitmentAnalyticsDto;
import com.skillset.application.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    ResponseEntity<AdminStatsDto> getStats(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(adminService.getStats(userId));
    }

    @GetMapping("/users")
    ResponseEntity<List<AdminStatsDto.UserSummary>> getUsers(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminService.searchUsers(userId, search));
    }

    @PutMapping("/users/{targetId}/status")
    ResponseEntity<Void> toggleUserStatus(
            @AuthenticationPrincipal String userId,
            @PathVariable String targetId) {
        adminService.toggleUserStatus(userId, targetId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/analytics")
    ResponseEntity<RecruitmentAnalyticsDto> getAnalytics(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(adminService.getRecruitmentAnalytics(userId));
    }

    @GetMapping("/jobs")
    ResponseEntity<List<AdminStatsDto.JobSummary>> listJobs(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminService.listAllJobs(userId, status));
    }

    @PutMapping("/jobs/{jobId}/status")
    ResponseEntity<AdminStatsDto.JobSummary> changeJobStatus(
            @AuthenticationPrincipal String userId,
            @PathVariable String jobId,
            @RequestParam String status) {
        return ResponseEntity.ok(adminService.changeJobStatus(userId, jobId, status));
    }

    @GetMapping("/export/applications")
    ResponseEntity<byte[]> exportApplicationsCsv(@AuthenticationPrincipal String userId) {
        byte[] csv = adminService.exportApplicationsCsv(userId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"candidatures.csv\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv);
    }
}
