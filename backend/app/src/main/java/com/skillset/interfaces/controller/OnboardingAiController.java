package com.skillset.interfaces.controller;

import com.skillset.application.dto.onboarding.*;
import com.skillset.application.service.CvGeneratorService;
import com.skillset.application.service.OnboardingAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller pour la génération du CV en fin d'onboarding.
 * Endpoint:
 * - POST /api/onboarding/generate-cv — Génère le CV depuis les réponses
 */
@Slf4j
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingAiController {

    private final OnboardingAiService onboardingAiService;
    private final CvGeneratorService cvGeneratorService;

    /**
     * POST /api/onboarding/generate-cv
     * Génère le CV professionnel depuis les réponses d'onboarding.
     *
     * Requête:
     * {
     *   "jobTitle": "Développeur Full Stack",
     *   "answers": [
     *     { "fieldKey": "country", "question": "Pays ?", "answer": "France", "phase": "INITIAL" },
     *     { "fieldKey": "city", "question": "Ville ?", "answer": "Paris", "phase": "INITIAL" },
     *     ...
     *   ]
     * }
     *
     * Réponse:
     * {
     *   "cvUrl": "https://res.cloudinary.com/.../cv_johndoe_1234567890.pdf",
     *   "message": "Votre CV a été généré et envoyé à votre email",
     *   "downloadUrl": "https://res.cloudinary.com/.../pdf",
     *   "emailSentTo": "john@example.com"
     * }
     */
    @PostMapping("/generate-cv")
    public ResponseEntity<GenerateCvResponseDTO> generateCv(
            @AuthenticationPrincipal String userId,
            @RequestBody GenerateCvRequestDTO request) {

        log.info("Génération CV pour user {} - poste: {}", userId, request.getJobTitle());

        try {
            // Générer structure CV depuis l'IA + réponses
            Map<String, Object> cvData = onboardingAiService.generateCvFromAnswers(
                    request.getJobTitle(),
                    request.getAnswers(),
                    "CANDIDATE"
            );

            // Orchestrer génération PDF + upload + email
            String cvUrl = cvGeneratorService.generateAndDeliverCv(userId, cvData);

            if (cvUrl == null) {
                return ResponseEntity.status(500)
                        .body(GenerateCvResponseDTO.builder()
                                .message("Erreur lors de la génération du CV")
                                .build());
            }

            return ResponseEntity.ok(GenerateCvResponseDTO.builder()
                    .cvUrl(cvUrl)
                    .downloadUrl(cvUrl)
                    .message("Votre CV a été généré avec succès et envoyé à votre email")
                    .emailSentTo("À vérifier dans vos emails")
                    .build());
        } catch (Exception e) {
            log.error("Erreur génération CV", e);
            return ResponseEntity.status(500)
                    .body(GenerateCvResponseDTO.builder()
                            .message("Erreur lors de la génération du CV")
                            .build());
        }
    }
}
