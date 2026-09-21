package com.skillset.interfaces.controller;

import com.skillset.application.dto.ReviewDTO;
import com.skillset.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER') or hasRole('ADMIN')")
public class ReviewController {

    private final ReviewService reviewService;

    record ReviewRequest(Integer rating, String comments, String status) {}

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewDTO> addReview(
            @AuthenticationPrincipal String userId,
            @PathVariable String id,
            @RequestBody ReviewRequest body) {
        return new ResponseEntity<>(
            reviewService.addReview(userId, id, body.rating(), body.comments(), body.status()),
            HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewDTO>> getReviews(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.getReviews(id));
    }
}
