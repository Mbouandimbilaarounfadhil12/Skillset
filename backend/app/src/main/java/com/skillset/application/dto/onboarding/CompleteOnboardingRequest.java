package com.skillset.application.dto.onboarding;

import lombok.Data;
import java.util.Map;

@Data
public class CompleteOnboardingRequest {
    private String role;
    private Map<String, String> initialAnswers;
    private Map<String, String> aiAnswers;
    /** candidates only */
    private boolean wantsCv;
}
