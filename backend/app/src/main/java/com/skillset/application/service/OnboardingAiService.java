package com.skillset.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.skillset.application.dto.onboarding.*;
import com.skillset.infrastructure.util.ContextualDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour la génération du CV en fin d'onboarding, avec contexte
 * cumulatif (villes/salaires/format CV adaptés au pays).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingAiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ContextualDataLoader contextualDataLoader;

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.model:gemini-flash-latest}")
    private String modelName;

    private static final String AI_API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    /**
     * Construit le contexte cumulatif depuis les réponses antérieures.
     * Détermine les options filtrées (villes, salaires, etc.) basées sur le pays.
     */
    public OnboardingContextDTO buildContext(List<PreviousAnswerDTO> previousAnswers) {
        OnboardingContextDTO.OnboardingContextDTOBuilder builder = OnboardingContextDTO.builder();

        String country = null;
        for (PreviousAnswerDTO answer : previousAnswers) {
            if ("country".equals(answer.getFieldKey())) {
                country = answer.getAnswer();
                break;
            }
        }

        if (country == null || country.isEmpty()) {
            // Pas de pays sélectionné = contexte vide, options bloquées
            return builder
                    .country(null)
                    .currency("EUR")
                    .currencySymbol("€")
                    .salaryPeriod("annuel")
                    .phonePrefix("")
                    .availableCities(Collections.emptyList())
                    .salaryRanges(Collections.emptyList())
                    .contractTypes(Collections.emptyList())
                    .workModes(Collections.emptyList())
                    .languages(Collections.emptyList())
                    .cvFormat(OnboardingContextDTO.CVFormatDTO.builder()
                            .withPhoto(false)
                            .withPersonalDetails(false)
                            .rgpdStrict(false)
                            .build())
                    .answeredFields(previousAnswers.stream()
                            .map(PreviousAnswerDTO::getFieldKey)
                            .collect(Collectors.toList()))
                    .build();
        }

        // Charger métadonnées pays
        Map<String, Object> countryData = contextualDataLoader.getCountryData(country);
        if (countryData == null) {
            countryData = new HashMap<>();
        }

        String currency = (String) countryData.getOrDefault("currency", "EUR");
        String currencySymbol = (String) countryData.getOrDefault("currencySymbol", "€");
        String salaryPeriod = (String) countryData.getOrDefault("salaryPeriod", "annuel");
        String phonePrefix = (String) countryData.getOrDefault("phonePrefix", "");

        List<String> cities = (List<String>) countryData.getOrDefault("cities", Collections.emptyList());
        List<String> salaryRanges = (List<String>) countryData.getOrDefault("salaryRanges", Collections.emptyList());
        List<String> contractTypes = (List<String>) countryData.getOrDefault("contractTypes", Collections.emptyList());
        List<String> workModes = (List<String>) countryData.getOrDefault("workModes", Collections.emptyList());
        List<String> languages = (List<String>) countryData.getOrDefault("languages", Collections.emptyList());

        boolean withPhoto = ((List<String>) countryData.getOrDefault("withPhoto", Collections.emptyList())).contains(country);
        boolean withDetails = ((List<String>) countryData.getOrDefault("withPersonalDetails", Collections.emptyList())).contains(country);
        boolean rgpdStrict = ((List<String>) countryData.getOrDefault("rgpdStrict", Collections.emptyList())).contains(country);

        return builder
                .country(country)
                .currency(currency)
                .currencySymbol(currencySymbol)
                .salaryPeriod(salaryPeriod)
                .phonePrefix(phonePrefix)
                .availableCities(cities)
                .salaryRanges(salaryRanges)
                .contractTypes(contractTypes)
                .workModes(workModes)
                .languages(languages)
                .cvFormat(OnboardingContextDTO.CVFormatDTO.builder()
                        .withPhoto(withPhoto)
                        .withPersonalDetails(withDetails)
                        .rgpdStrict(rgpdStrict)
                        .build())
                .answeredFields(previousAnswers.stream()
                        .map(PreviousAnswerDTO::getFieldKey)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Génère le CV structuré depuis les réponses de l'onboarding.
     * Appelle le fournisseur IA configuré avec toutes les réponses + contexte pays.
     */
    public Map<String, Object> generateCvFromAnswers(String jobTitle, List<PreviousAnswerDTO> answers, String userRole) {
        OnboardingContextDTO context = buildContext(answers);

        String cvPrompt = buildCvPrompt(jobTitle, answers, context, userRole);

        try {
            String responseText = callAiProvider(cvPrompt);
            // Parser JSON retourné
            Map<String, Object> cvData = objectMapper.readValue(responseText, Map.class);
            enrichCvWithCountryFormat(cvData, context);
            return cvData;
        } catch (Exception e) {
            log.error("Erreur génération CV IA", e);
            return generateFallbackCvStructure(jobTitle, answers);
        }
    }

    // ─── PRIVATE HELPERS ───

    private String buildCvPrompt(String jobTitle, List<PreviousAnswerDTO> answers,
                                  OnboardingContextDTO context, String userRole) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Génère un CV professionnel ATS 2024 pour un ").append(jobTitle)
                .append(" en ").append(context.getCountry()).append(".\n\n");

        prompt.append("FORMAT selon pays:\n");
        prompt.append("- Photo: ").append(context.getCvFormat().isWithPhoto() ? "INCLURE" : "NE PAS INCLURE").append("\n");
        prompt.append("- Âge/nationalité: ").append(context.getCvFormat().isWithPersonalDetails() ? "INCLURE" : "EXCLURE (RGPD)").append("\n");
        prompt.append("- Devise: ").append(context.getCurrencySymbol()).append("\n\n");

        prompt.append("Réponses de l'utilisateur:\n");
        for (PreviousAnswerDTO answer : answers) {
            prompt.append("- ").append(answer.getFieldKey()).append(": ").append(answer.getAnswer()).append("\n");
        }

        prompt.append("\nRéponds UNIQUEMENT en JSON valide avec structure CV complète.\n");

        return prompt.toString();
    }

    private String callAiProvider(String userPrompt) throws Exception {
        if (aiApiKey == null || aiApiKey.isEmpty()) {
            log.warn("AI provider API key not configured — using fallback");
            return "{}";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", aiApiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("system_instruction", Map.of("parts", List.of(Map.of("text", "Tu es un expert RH."))));
        body.put("contents", List.of(
                Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))
        ));
        body.put("generationConfig", Map.of(
                "maxOutputTokens", 2000,
                "thinkingConfig", Map.of("thinkingBudget", 0)
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String response = restTemplate.postForObject(
                String.format(AI_API_URL_TEMPLATE, modelName),
                entity,
                String.class
        );

        JsonNode root = objectMapper.readTree(response);
        return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
    }

    private void enrichCvWithCountryFormat(Map<String, Object> cvData, OnboardingContextDTO context) {
        cvData.put("country", context.getCountry());
        cvData.put("currency", context.getCurrency());
        cvData.put("cvFormat", Map.of(
                "withPhoto", context.getCvFormat().isWithPhoto(),
                "withPersonalDetails", context.getCvFormat().isWithPersonalDetails(),
                "rgpdStrict", context.getCvFormat().isRgpdStrict()
        ));
    }

    private Map<String, Object> generateFallbackCvStructure(String jobTitle, List<PreviousAnswerDTO> answers) {
        Map<String, Object> cv = new HashMap<>();
        cv.put("title", jobTitle);
        cv.put("answers", answers);
        return cv;
    }
}
