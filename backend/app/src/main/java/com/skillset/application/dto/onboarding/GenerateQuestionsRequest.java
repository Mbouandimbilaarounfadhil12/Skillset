package com.skillset.application.dto.onboarding;

import lombok.Data;
import java.util.Map;

@Data
public class GenerateQuestionsRequest {
    /** CANDIDATE or EMPLOYER */
    private String role;
    /** key = question id, value = answer text / selected option(s) */
    private Map<String, String> initialAnswers;
}
