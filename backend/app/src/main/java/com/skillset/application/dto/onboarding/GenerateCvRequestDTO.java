package com.skillset.application.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO pour requête de génération de CV
 * POST /api/onboarding/generate-cv
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateCvRequestDTO {

    private String jobTitle; // Titre du poste recherché
    private List<PreviousAnswerDTO> answers; // Toutes les réponses de l'onboarding
}
