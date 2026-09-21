package com.skillset.interfaces.controller;

import com.skillset.application.dto.AuthResponse;
import com.skillset.application.dto.onboarding.CompleteOnboardingRequest;
import com.skillset.application.dto.onboarding.GenerateQuestionsRequest;
import com.skillset.application.dto.onboarding.GenerateQuestionsResponse;
import com.skillset.application.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping("/generate-questions")
    ResponseEntity<GenerateQuestionsResponse> generateQuestions(@RequestBody GenerateQuestionsRequest request) {
        return ResponseEntity.ok(onboardingService.generateQuestions(request));
    }

    @PostMapping("/complete")
    ResponseEntity<AuthResponse> completeOnboarding(
            @AuthenticationPrincipal String userId,
            @RequestBody CompleteOnboardingRequest request) {
        return ResponseEntity.ok(onboardingService.completeOnboarding(userId, request));
    }
}
