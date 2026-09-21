package com.skillset.application.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {
    private String id;
    private String text;
    /** single_choice | multi_choice | text | number */
    private String type;
    private List<String> options;
    private String placeholder;
}
