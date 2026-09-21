package com.skillset.application.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO représentant le contexte cumulatif pour le frontend
 * Permet au frontend de connaître les options filtrées (villes, salaires, etc.)
 * GET /api/onboarding/context
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingContextDTO {

    // Données pays sélectionné
    private String country;
    private String currency;
    private String currencySymbol;
    private String salaryPeriod; // "mensuel" ou "annuel"
    private String phonePrefix; // Ex: "+237" pour Cameroun

    // Options filtrées basées sur le pays choisi
    private List<String> availableCities;
    private List<String> salaryRanges;
    private List<String> contractTypes;
    private List<String> workModes;
    private List<String> languages;

    // Format CV selon pays
    private CVFormatDTO cvFormat;

    // Champs déjà répondus (pour éviter de les reposer)
    private List<String> answeredFields;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CVFormatDTO {
        private boolean withPhoto;
        private boolean withPersonalDetails;
        private boolean rgpdStrict;
    }
}
