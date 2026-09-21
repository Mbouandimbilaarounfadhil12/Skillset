package com.skillset.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationAnswerDTO {
    private String id;
    private String applicationId;
    private String screeningQuestionId;
    private String answerText;
}
