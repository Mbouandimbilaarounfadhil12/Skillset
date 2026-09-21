package com.skillset.application.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO représentant une réponse antérieure dans le flux onboarding
 * Utilisé pour reconstruire le contexte cumulatif
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviousAnswerDTO {

    private String fieldKey; // Clé unique de la question (ex: "country", "salary", "experience")
    private String question; // Texte de la question posée
    private String answer; // La réponse fournie par l'utilisateur
    private String phase; // Phase dans laquelle cette réponse a été donnée (INTRO, LOCALISATION, etc.)
}
