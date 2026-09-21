package com.skillset.interfaces.controller;

import com.skillset.application.service.InterviewService;
import com.skillset.domain.entity.Interview;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {
    private final InterviewService interviewService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Interview> scheduleInterview(@AuthenticationPrincipal String userId,
                                                     @RequestBody Interview interview) {
        Interview scheduledInterview = interviewService.scheduleInterview(userId, interview);
        return new ResponseEntity<>(scheduledInterview, HttpStatus.CREATED);
    }

    @GetMapping("/candidate/{candidateId}")
    @PreAuthorize("hasRole('ADMIN') or #candidateId == authentication.principal")
    public ResponseEntity<List<Interview>> getCandidateInterviews(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String candidateId) {
        List<Interview> interviews = interviewService.getCandidateInterviews(currentUserId, candidateId);
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/interviewer/{interviewerId}")
    @PreAuthorize("hasRole('ADMIN') or #interviewerId == authentication.principal")
    public ResponseEntity<List<Interview>> getInterviewerSchedule(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String interviewerId) {
        List<Interview> interviews = interviewService.getInterviewerSchedule(currentUserId, interviewerId);
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/{interviewId}")
    public ResponseEntity<Interview> getInterviewById(@AuthenticationPrincipal String currentUserId,
                                                        @PathVariable String interviewId) {
        var interview = interviewService.getInterviewById(currentUserId, interviewId);
        if (interview.isPresent()) {
            return ResponseEntity.ok(interview.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{interviewId}/status")
    public ResponseEntity<Interview> updateInterviewStatus(@AuthenticationPrincipal String currentUserId,
                                                           @PathVariable String interviewId,
                                                           @RequestParam String status) {
        Interview updatedInterview = interviewService.updateInterviewStatus(currentUserId, interviewId, status);
        if (updatedInterview != null) {
            return ResponseEntity.ok(updatedInterview);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{interviewId}/feedback")
    public ResponseEntity<Interview> addFeedback(@AuthenticationPrincipal String currentUserId,
                                                 @PathVariable String interviewId,
                                                 @RequestParam String notes,
                                                 @RequestParam Integer rating) {
        Interview updatedInterview = interviewService.addInterviewFeedback(
                currentUserId, interviewId, notes, rating);
        if (updatedInterview != null) {
            return ResponseEntity.ok(updatedInterview);
        }
        return ResponseEntity.notFound().build();
    }
}
