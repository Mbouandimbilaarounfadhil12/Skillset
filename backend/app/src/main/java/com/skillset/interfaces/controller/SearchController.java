package com.skillset.interfaces.controller;

import com.skillset.application.dto.*;
import com.skillset.application.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/jobs")
    public ResponseEntity<Page<ScoredJobDTO>> searchJobs(@RequestBody JobSearchCriteria criteria,
                                                           @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(searchService.searchJobs(criteria, userId));
    }

    @PostMapping("/candidates")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Page<ScoredCandidateDTO>> searchCandidates(
            @RequestBody CandidateSearchCriteria criteria,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(searchService.searchCandidates(criteria));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(@RequestParam String q) {
        return ResponseEntity.ok(searchService.getSuggestions(q));
    }
}
