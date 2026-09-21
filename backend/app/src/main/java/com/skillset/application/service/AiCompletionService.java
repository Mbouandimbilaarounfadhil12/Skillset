package com.skillset.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillset.application.dto.MatchResult;
import com.skillset.application.dto.onboarding.QuestionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AiCompletionService {

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.model:gemini-flash-latest}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String AI_API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final String NON_PRECISE = "Non précisé";

    // ── Analyse de matching CV / offre ────────────────────────────────────────

    /**
     * Envoie le texte du CV et les exigences du poste au fournisseur IA configuré.
     * Retourne Optional.empty() si la clé API est absente ou en cas d'erreur.
     */
    public Optional<MatchResult> analyzeMatch(String cvText, String jobRequirements) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI provider API key absent — matching keyword-only utilisé en fallback.");
            return Optional.empty();
        }

        // Tronquer les textes pour limiter les tokens (~4k chars max chacun)
        String cv  = cvText      != null ? cvText.substring(0, Math.min(cvText.length(),      4000)) : "";
        String job = jobRequirements != null
                ? jobRequirements.substring(0, Math.min(jobRequirements.length(), 2000)) : "";

        String prompt =
            "Tu es un expert en recrutement. Évalue la correspondance entre ce CV et ce poste.\n\n"
            + "=== CV DU CANDIDAT ===\n" + cv + "\n\n"
            + "=== COMPÉTENCES ET DESCRIPTION DU POSTE ===\n" + job + "\n\n"
            + "Réponds UNIQUEMENT avec un objet JSON valide, sans markdown, sans texte avant ou après :\n"
            + "{\"score\": <entier 0-100>, \"explanation\": \"<1-2 phrases concises en français>\"}\n\n"
            + "Barème :\n"
            + "80-100 : compétences principales toutes présentes, profil idéal\n"
            + "60-79  : bon match, quelques lacunes mineures\n"
            + "40-59  : match partiel, lacunes notables\n"
            + "0-39   : peu de correspondance";

        try {
            String text = callAndExtractText(prompt, 256).trim();
            text = text.replace("```json", "").replace("```", "").trim();

            JsonNode json  = objectMapper.readTree(text);
            double   score = json.path("score").asDouble(0);
            String   expl  = json.path("explanation").asText("");

            return Optional.of(new MatchResult(Math.min(100, Math.max(0, score)), expl));
        } catch (Exception e) {
            log.error("Échec de l'appel IA pour analyzeMatch : {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ── Génération de questions d'onboarding ─────────────────────────────────

    public List<QuestionDto> generateCandidateQuestions(Map<String, String> initialAnswers) {
        String prompt = buildCandidatePrompt(initialAnswers);
        return callAiAndParse(prompt);
    }

    public List<QuestionDto> generateEmployerQuestions(Map<String, String> initialAnswers) {
        String prompt = buildEmployerPrompt(initialAnswers);
        return callAiAndParse(prompt);
    }

    private List<QuestionDto> callAiAndParse(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI provider API key not configured — returning fallback questions");
            return fallbackQuestions();
        }
        try {
            String text = callAndExtractText(prompt, 2500);

            // strip optional markdown code block
            text = text.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(text, new TypeReference<List<QuestionDto>>() {});
        } catch (Exception e) {
            log.error("AI provider call failed: {}", e.getMessage());
            return fallbackQuestions();
        }
    }

    private String callAndExtractText(String prompt, int maxOutputTokens) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
            "generationConfig", Map.of(
                "maxOutputTokens", maxOutputTokens,
                "thinkingConfig", Map.of("thinkingBudget", 0)
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                String.format(AI_API_URL_TEMPLATE, model), entity, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
    }

    private String buildCandidatePrompt(Map<String, String> answers) {
        String domain   = answers.getOrDefault("domain", NON_PRECISE);
        String role     = answers.getOrDefault("desiredRole", NON_PRECISE);
        String level    = answers.getOrDefault("experienceLevel", NON_PRECISE);
        String contract = answers.getOrDefault("contractType", NON_PRECISE);
        String country  = answers.getOrDefault("candidateCountry", NON_PRECISE);
        String city     = answers.getOrDefault("candidateCity", NON_PRECISE);

        boolean isRemote = contract.toLowerCase().contains("remote")
            || contract.toLowerCase().contains("freelance")
            || answers.values().stream().anyMatch(v ->
                v.toLowerCase().contains("remote") || v.toLowerCase().contains("full remote"));

        String alreadyKnown = answers.entrySet().stream()
            .map(e -> "- " + e.getKey() + " : " + e.getValue())
            .collect(java.util.stream.Collectors.joining("\n"));

        String geoNote = isRemote
            ? "Ce candidat travaille en REMOTE/FREELANCE — ne pose aucune question sur la localisation "
              + "physique. Pose des questions sur l'organisation async, les outils distants, le fuseau horaire."
            : "Ce candidat est en présentiel/hybride dans " + country + " (" + city + "). "
              + "Adapte la monnaie (" + localCurrency(country) + "), les villes et les références locales.";

        return "Tu es un expert en matching emploi pour la plateforme SkillSet.\n"
            + "Objectif : construire le profil le plus PRÉCIS possible du candidat pour optimiser "
            + "son matching avec les offres dans le domaine \"" + domain + "\".\n\n"
            + "=== CONTEXTE DU CANDIDAT (DÉJÀ COLLECTÉ — NE PAS REDEMANDER) ===\n"
            + "Les informations suivantes ont déjà été obtenues. Analyse-les attentivement :\n"
            + alreadyKnown + "\n\n"
            + "ANTI-REDONDANCE ABSOLUE : Avant de générer chaque question, vérifie qu'elle ne porte pas "
            + "sur une information déjà présente dans le contexte ci-dessus, même formulée différemment. "
            + "Domaine, poste, niveau, contrat, pays, ville, LinkedIn, autorisation de travail, "
            + "et références sont DÉJÀ CONNUS — ne les redemande pas.\n\n"
            + "=== CONTEXTE GÉOGRAPHIQUE ===\n"
            + geoNote + "\n\n"
            + "=== INSTRUCTIONS DE GÉNÉRATION ===\n"
            + "Génère entre 8 et 14 questions selon la complexité du métier \"" + domain + "\".\n"
            + "Chaque question doit apporter une information UNIQUE et NON REDONDANTE pour affiner le matching.\n"
            + "Toutes les options doivent correspondre à la réalité de \"" + country + "\" et du domaine \"" + domain + "\".\n\n"
            + "TYPES DE QUESTIONS (adapte selon le domaine \"" + domain + "\") :\n"
            + "- Compétences techniques réelles (multi_choice) : 8-12 options propres au domaine et niveau\n"
            + "- Types d'employeurs cibles (multi_choice) : adaptés au domaine et au pays\n"
            + "- Réalisation professionnelle marquante (text) : question comportementale ouverte\n"
            + "- Disponibilité et mobilité selon contexte remote/présentiel\n"
            + "- Prétentions salariales en " + localCurrency(country) + " (text avec placeholder réaliste)\n"
            + "- Formations et certifications reconnues dans \"" + country + "\" (multi_choice)\n"
            + "- Environnement de travail préféré (single_choice)\n"
            + "- Langues professionnelles (multi_choice)\n"
            + "- Questions spécifiques au métier \"" + domain + "\" (1-4 questions propres à ce secteur)\n"
            + "- Questions de traçabilité/sécurité (inclure 1-2 parmi les suivantes SI non déjà renseigné) :\n"
            + "    • Numéro/type de pièce d'identité disponible pour vérification (single_choice)\n"
            + "    • Références professionnelles joignables : nombre et qualité (single_choice)\n"
            + "    • Autorisation légale de travailler dans \"" + country + "\" : statut précis (single_choice)\n\n"
            + "RÈGLES ABSOLUES :\n"
            + "- JAMAIS de numéro dans le texte d'une question (pas de \"1.\", \"Q1\", \"Question 1\", etc.)\n"
            + "- JAMAIS de question générique applicable à tout le monde\n"
            + "- JAMAIS de question déjà couverte par le contexte DÉJÀ COLLECTÉ ci-dessus\n"
            + "- Les options sont TOUJOURS spécifiques au pays \"" + country + "\"\n\n"
            + "FORMAT JSON STRICT (sans markdown, sans texte avant/après) :\n"
            + "[{\"id\":\"q1\",\"text\":\"...\",\"type\":\"text|single_choice|multi_choice\","
            + "\"options\":null,\"placeholder\":null}]";
    }

    private String buildEmployerPrompt(Map<String, String> answers) {
        String company      = answers.getOrDefault("companyName", NON_PRECISE);
        String industry     = answers.getOrDefault("industry", NON_PRECISE);
        String size         = answers.getOrDefault("companySize", NON_PRECISE);
        String hiringRole   = answers.getOrDefault("hiringRole", NON_PRECISE);
        String country      = answers.getOrDefault("companyCountry", NON_PRECISE);
        String city         = answers.getOrDefault("companyCity", NON_PRECISE);
        String location     = city.equals(NON_PRECISE) ? country : (country.equals(NON_PRECISE) ? city : city + ", " + country);
        String contractType = answers.getOrDefault("contractType", NON_PRECISE);

        boolean isRemote = contractType.toLowerCase().contains("remote")
            || answers.values().stream().anyMatch(v ->
                v.toLowerCase().contains("remote") || v.toLowerCase().contains("full remote"));

        String alreadyKnown = answers.entrySet().stream()
            .map(e -> "- " + e.getKey() + " : " + e.getValue())
            .collect(java.util.stream.Collectors.joining("\n"));

        String geoNote = isRemote
            ? "Le poste est en REMOTE — ne pose aucune question sur la présence physique. "
              + "Pose des questions sur l'organisation async, les outils distants, la couverture horaire."
            : "Le poste est en présentiel/hybride à " + location + ". "
              + "Adapte la monnaie (" + localCurrency(country) + "), les normes légales et les références locales.";

        return "Tu es un expert en matching emploi pour la plateforme SkillSet.\n"
            + "Objectif : construire le profil PRÉCIS du besoin de recrutement de l'entreprise \""
            + company + "\" pour optimiser le matching avec les candidats.\n\n"
            + "=== CONTEXTE DE L'OFFRE (DÉJÀ COLLECTÉ — NE PAS REDEMANDER) ===\n"
            + "Les informations suivantes ont déjà été obtenues. Analyse-les attentivement :\n"
            + alreadyKnown + "\n\n"
            + "ANTI-REDONDANCE ABSOLUE : Avant de générer chaque question, vérifie qu'elle ne porte pas "
            + "sur une information déjà présente dans le contexte ci-dessus, même formulée différemment. "
            + "Nom d'entreprise, secteur, taille, localisation, poste, contrat, numéro d'enregistrement, "
            + "site web, LinkedIn et adresse sont DÉJÀ CONNUS — ne les redemande pas.\n\n"
            + "=== CONTEXTE GÉOGRAPHIQUE ===\n"
            + geoNote + "\n\n"
            + "=== INSTRUCTIONS DE GÉNÉRATION ===\n"
            + "Génère entre 8 et 12 questions selon la spécificité du poste \"" + hiringRole + "\".\n"
            + "Chaque question doit apporter une information UNIQUE et NON REDONDANTE pour le matching candidat/offre.\n"
            + "Toutes les options doivent correspondre à la réalité de \"" + country + "\" et du secteur \"" + industry + "\".\n\n"
            + "TYPES DE QUESTIONS (adapte selon le secteur \"" + industry + "\") :\n"
            + "- Compétences techniques indispensables (multi_choice) : 8-12 options propres au poste\n"
            + "- Niveau et profil d'expérience recherché (single_choice)\n"
            + "- Missions principales concrètes (text) : question ouverte sur le quotidien du rôle\n"
            + "- Rémunération proposée en " + localCurrency(country) + " (text avec placeholder réaliste)\n"
            + "- Culture et environnement de travail (single_choice)\n"
            + "- Questions spécifiques au secteur \"" + industry + "\" (2-4 questions propres à ce secteur)\n"
            + "- Critères différenciants pour le profil idéal (text ou multi_choice)\n"
            + "- Calendrier et processus de recrutement (text)\n\n"
            + "RÈGLES ABSOLUES :\n"
            + "- JAMAIS de numéro dans le texte d'une question (pas de \"1.\", \"Q1\", \"Question 1\", etc.)\n"
            + "- JAMAIS de question générique applicable à toutes les entreprises\n"
            + "- JAMAIS de question déjà couverte par le contexte DÉJÀ COLLECTÉ ci-dessus\n"
            + "- Les options sont TOUJOURS spécifiques au pays \"" + country + "\" et au secteur \"" + industry + "\"\n"
            + "- PAS de question de vérification de légitimité (déjà traitée en étape 1)\n\n"
            + "FORMAT JSON STRICT (sans markdown, sans texte avant/après) :\n"
            + "[{\"id\":\"q1\",\"text\":\"...\",\"type\":\"text|single_choice|multi_choice|number\","
            + "\"options\":null,\"placeholder\":null}]";
    }

    private String localCurrency(String country) {
        return switch (country) {
            case "France", "Belgique", "Luxembourg", "Espagne", "Portugal",
                 "Italie", "Allemagne", "Pays-Bas" -> "euros (€)";
            case "Suisse"   -> "francs suisses (CHF)";
            case "Royaume-Uni" -> "livres sterling (GBP)";
            case "Canada"   -> "dollars canadiens (CAD)";
            case "États-Unis" -> "dollars américains (USD)";
            case "Australie" -> "dollars australiens (AUD)";
            case "Maroc"    -> "dirhams (MAD)";
            case "Tunisie"  -> "dinars tunisiens (TND)";
            case "Algérie"  -> "dinars algériens (DZD)";
            case "Émirats Arabes Unis" -> "dirhams (AED)";
            case "Qatar"    -> "riyals (QAR)";
            case "Inde"     -> "roupies (INR)";
            case "Chine"    -> "yuans (CNY)";
            case "Japon"    -> "yens (JPY)";
            case "Brésil"   -> "reais (BRL)";
            default         -> "FCFA (XAF)";
        };
    }

    private List<QuestionDto> fallbackQuestions() {
        return List.of(
            new QuestionDto("q1", "Quelles sont vos principales compétences ?", "text", null, "Ex : Python, gestion de projet, comptabilité..."),
            new QuestionDto("q2", "Quel salaire annuel brut souhaitez-vous ?", "text", null, "Ex : 45 000 €"),
            new QuestionDto("q3", "Êtes-vous ouvert(e) au télétravail ?", "single_choice",
                List.of("100% présentiel", "Hybride (2-3j/semaine)", "Full remote"), null),
            new QuestionDto("q4", "Dans quelle(s) ville(s) souhaitez-vous travailler ?", "text", null, "Ex : Paris, Lyon, Bordeaux"),
            new QuestionDto("q5", "Quelle est votre disponibilité ?", "single_choice",
                List.of("Immédiate", "1 mois", "2 mois", "3 mois et plus"), null),
            new QuestionDto("q6", "Avez-vous des certifications ou formations notables ?", "text", null, "Ex : AWS Certified, PMP, Master RH...")
        );
    }
}
