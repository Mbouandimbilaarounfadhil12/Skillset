package com.skillset.application.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour réponse de génération de CV
 * Retourné par POST /api/onboarding/generate-cv
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateCvResponseDTO {

    private String cvUrl; // URL Cloudinary du CV généré
    private String message; // Message de confirmation
    private String downloadUrl; // URL directe pour télécharger le PDF
    private String emailSentTo; // Email auquel le CV a été envoyé
}
