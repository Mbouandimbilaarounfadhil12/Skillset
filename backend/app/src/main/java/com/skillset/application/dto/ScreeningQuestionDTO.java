package com.skillset.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningQuestionDTO {
    private String id;
    private String jobListingId;
    private String questionText;
    private String questionType;
    private String options;
    private Boolean isRequired;
    private Integer orderIndex;
}
