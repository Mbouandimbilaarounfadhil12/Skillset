package com.skillset.interfaces.controller;

import com.skillset.application.dto.AutomationRuleDTO;
import com.skillset.application.service.AutomationService;
import com.skillset.domain.entity.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/automation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER')")
public class AutomationController {

    private final AutomationService automationService;

    @PostMapping("/rules")
    public ResponseEntity<AutomationRuleDTO> createRule(
            @RequestBody AutomationRuleDTO dto,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(automationService.createRule(dto, userId));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<AutomationRuleDTO>> getRules(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(automationService.getRulesForEmployer(userId));
    }

    @PutMapping("/rules/{id}/toggle")
    public ResponseEntity<AutomationRuleDTO> toggleRule(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(automationService.toggleRule(id, userId));
    }

    @PostMapping("/bulk-status")
    public ResponseEntity<Map<String, Object>> bulkUpdateStatus(
            @RequestBody BulkStatusRequest body,
            @AuthenticationPrincipal String userId) {
        ApplicationStatus status = ApplicationStatus.valueOf(body.newStatus().toUpperCase());
        return ResponseEntity.ok(automationService.bulkUpdateStatus(body.applicationIds(), status, userId));
    }

    record BulkStatusRequest(List<String> applicationIds, String newStatus) {}
}
