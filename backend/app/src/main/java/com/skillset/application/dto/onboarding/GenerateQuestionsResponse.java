package com.skillset.application.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class GenerateQuestionsResponse {
    private List<QuestionDto> questions;
}
