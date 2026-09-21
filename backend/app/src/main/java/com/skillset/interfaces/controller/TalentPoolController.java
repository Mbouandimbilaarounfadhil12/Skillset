package com.skillset.interfaces.controller;

import com.skillset.application.dto.ScoredCandidateDTO;
import com.skillset.application.dto.TalentPoolDTO;
import com.skillset.application.dto.TalentPoolMemberDTO;
import com.skillset.application.dto.TalentPoolSummaryDTO;
import com.skillset.application.service.TalentPoolService;
import com.skillset.domain.entity.TalentPoolMemberSource;
import com.skillset.domain.entity.TalentPoolMemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/talent-pools")
@RequiredArgsConstructor
public class TalentPoolController {
    private final TalentPoolService talentPoolService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<TalentPoolDTO> createPool(
            @RequestBody TalentPoolDTO dto,
            @AuthenticationPrincipal String userId) {
        TalentPoolDTO createdPool = talentPoolService.createPool(dto, userId);
        return new ResponseEntity<>(createdPool, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<List<TalentPoolSummaryDTO>> getPoolsForEmployer(
            @AuthenticationPrincipal String userId) {
        List<TalentPoolSummaryDTO> pools = talentPoolService.getPoolsForEmployer(userId);
        return ResponseEntity.ok(pools);
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Page<TalentPoolMemberDTO>> getPoolMembers(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TalentPoolMemberDTO> members = talentPoolService.getPoolMembers(id, userId, pageable);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Void> addCandidate(
            @PathVariable String id,
            @RequestBody AddCandidateRequest req,
            @AuthenticationPrincipal String userId) {
        talentPoolService.addCandidate(
                id,
                req.candidateId.toString(),
                req.notes,
                req.source,
                userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/{id}/members/{candidateId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Void> removeCandidate(
            @PathVariable String id,
            @PathVariable String candidateId,
            @AuthenticationPrincipal String userId) {
        talentPoolService.removeCandidate(id, candidateId, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}/members/{candidateId}/status")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Void> updateMemberStatus(
            @PathVariable String id,
            @PathVariable String candidateId,
            @RequestBody UpdateStatusRequest req,
            @AuthenticationPrincipal String userId) {
        talentPoolService.updateMemberStatus(id, candidateId, req.status, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{id}/members/from-application")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Void> addFromApplication(
            @PathVariable String id,
            @RequestBody AddFromAppRequest req,
            @AuthenticationPrincipal String userId) {
        talentPoolService.addFromApplication(req.applicationId.toString(), id, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{id}/recommendations")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<List<ScoredCandidateDTO>> getRecommendedCandidates(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        List<ScoredCandidateDTO> recommendations = talentPoolService.getRecommendedCandidates(id, userId);
        return ResponseEntity.ok(recommendations);
    }

    public record AddCandidateRequest(UUID candidateId, String notes, TalentPoolMemberSource source) {}

    public record UpdateStatusRequest(TalentPoolMemberStatus status) {}

    public record AddFromAppRequest(UUID applicationId) {}
}
