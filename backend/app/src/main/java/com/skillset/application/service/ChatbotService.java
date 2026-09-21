package com.skillset.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillset.application.dto.ChatRequest;
import com.skillset.domain.entity.CandidateProfile;
import com.skillset.domain.entity.EmployerProfile;
import com.skillset.domain.entity.User;
import com.skillset.domain.entity.UserRole;
import com.skillset.domain.port.EmployerProfileRepositoryPort;
import com.skillset.infrastructure.persistence.CandidateProfileRepository;
import com.skillset.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.model:gemini-flash-latest}")
    private String model;

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepositoryPort employerProfileRepositoryPort;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String AI_API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    public String chat(String userId, ChatRequest request) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "Désolé, impossible de vous identifier. Veuillez vous reconnecter.";
        }
        String systemPrompt = buildSystemPrompt(user, request.getProfile());
        return callAiProvider(systemPrompt, request.getHistory(), request.getMessage());
    }

    private String buildSystemPrompt(User user, Map<String, Object> profile) {
        String roleLabel = user.getRole() == UserRole.CANDIDATE ? "candidat(e)"
                         : user.getRole() == UserRole.EMPLOYER  ? "employeur"
                         : "administrateur";

        StringBuilder sb = new StringBuilder();
        sb.append("Tu es STELLA, l'assistante IA intelligente et bienveillante de la plateforme SkillSet.\n");
        sb.append("SkillSet est une plateforme de recrutement qui connecte talents et entreprises.\n\n");
        sb.append("Utilisateur : ").append(user.getFirstName()).append(" ").append(user.getLastName())
          .append(" (").append(roleLabel).append(")\n\n");

        if (user.getRole() == UserRole.CANDIDATE) {
            sb.append("En tant qu'assistante d'un candidat, tu peux :\n");
            sb.append("• Analyser son profil et suggérer des améliorations\n");
            sb.append("• Rechercher des offres d'emploi adaptées à son profil\n");
            sb.append("• Estimer son salaire selon son expérience et son domaine\n");
            sb.append("• Préparer des entretiens d'embauche (méthode STAR, questions fréquentes)\n");
            sb.append("• Aider à rédiger des lettres de motivation et optimiser son CV\n");
            sb.append("• Conseiller sur la recherche d'emploi et la négociation salariale\n");
        } else if (user.getRole() == UserRole.EMPLOYER) {
            sb.append("En tant qu'assistante d'un employeur, tu peux :\n");
            sb.append("• Aider à rédiger des offres d'emploi attractives et inclusives\n");
            sb.append("• Analyser les candidatures reçues et établir des critères de sélection\n");
            sb.append("• Préparer des questions d'entretien pertinentes\n");
            sb.append("• Donner des conseils sur le recrutement et la rétention des talents\n");
            sb.append("• Analyser le marché du travail et les tendances salariales\n");
            sb.append("• Aider à définir des fiches de poste et des grilles de rémunération\n");
        }

        boolean hasServerProfile = appendServerProfile(sb, user);

        if (profile != null && !profile.isEmpty()) {
            sb.append("\nContexte additionnel transmis par l'application :\n");
            profile.forEach((k, v) -> {
                if (v != null && !String.valueOf(v).isBlank()
                        && !k.equals("id") && !k.equals("password")
                        && !k.equals("onboardingCompleted")) {
                    sb.append("- ").append(k).append(" : ").append(v).append("\n");
                }
            });
        }

        if (hasServerProfile) {
            sb.append("\nTu as déjà accès aux informations de profil ci-dessus. ")
              .append("NE redemande JAMAIS à l'utilisateur de préciser son domaine, son expérience, ")
              .append("ses compétences ou sa localisation s'ils figurent déjà dans ce contexte — ")
              .append("utilise-les directement pour personnaliser ta réponse.\n");
        }

        sb.append("\nRègles de réponse :\n");
        sb.append("- Réponds TOUJOURS en français\n");
        sb.append("- Sois concis, professionnel et bienveillant\n");
        sb.append("- Utilise **gras** pour les points importants\n");
        sb.append("- Utilise • pour les listes\n");
        sb.append("- Limite tes réponses à 300 mots sauf si une explication détaillée est demandée\n");
        sb.append("- Si tu manques d'informations précises, pose une question de clarification courte\n");
        sb.append("- Ne réponds pas aux questions sans rapport avec l'emploi, le recrutement ou SkillSet\n");

        return sb.toString();
    }

    /**
     * Injecte le profil réel de l'utilisateur (issu de l'onboarding) dans le prompt système,
     * pour que STELLA connaisse déjà ces informations et n'ait pas besoin de les redemander.
     */
    private boolean appendServerProfile(StringBuilder sb, User user) {
        if (user.getRole() == UserRole.CANDIDATE) {
            Optional<CandidateProfile> profileOpt = candidateProfileRepository.findByUserId(user.getId());
            if (profileOpt.isEmpty()) return false;
            CandidateProfile p = profileOpt.get();
            sb.append("\nProfil du candidat (déjà connu, ne pas redemander) :\n");
            appendIfPresent(sb, "Domaine professionnel", p.getJobDomain());
            appendIfPresent(sb, "Poste recherché", p.getDesiredRole());
            appendIfPresent(sb, "Niveau d'expérience", p.getExperienceLevel());
            appendIfPresent(sb, "Type de contrat souhaité", p.getContractType());
            appendIfPresent(sb, "Localisation", p.getLocation() != null ? p.getLocation()
                    : String.join(", ", nvl(p.getCity()), nvl(p.getCountry())));
            appendIfPresent(sb, "Compétences", p.getSkills());
            appendIfPresent(sb, "Bio", p.getBio());
            return true;
        }
        if (user.getRole() == UserRole.EMPLOYER) {
            Optional<EmployerProfile> profileOpt = employerProfileRepositoryPort.findByUserId(user.getId());
            if (profileOpt.isEmpty()) return false;
            EmployerProfile p = profileOpt.get();
            sb.append("\nProfil de l'entreprise (déjà connu, ne pas redemander) :\n");
            appendIfPresent(sb, "Entreprise", p.getCompanyName());
            appendIfPresent(sb, "Secteur", p.getIndustry());
            appendIfPresent(sb, "Taille", p.getCompanySize());
            appendIfPresent(sb, "Localisation", String.join(", ", nvl(p.getCompanyCity()), nvl(p.getCompanyCountry())));
            return true;
        }
        return false;
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank() && !value.equals(", ")) {
            sb.append("- ").append(label).append(" : ").append(value).append("\n");
        }
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private String callAiProvider(String systemPrompt, List<Map<String, String>> history, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI provider API key not configured — returning fallback reply for STELLA");
            return fallbackReply();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            // Build contents: history (last 10, assistant→model) + current message
            List<Map<String, Object>> contents = new ArrayList<>();
            if (history != null) {
                history.stream()
                    .filter(m -> "user".equals(m.get("role")) || "assistant".equals(m.get("role")))
                    .limit(10)
                    .forEach(m -> {
                        String role = "assistant".equals(m.get("role")) ? "model" : "user";
                        contents.add(Map.of("role", role, "parts", List.of(Map.of("text", m.get("content")))));
                    });
            }
            contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", message))));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
            body.put("contents", contents);
            body.put("generationConfig", Map.of(
                    "maxOutputTokens", 800,
                    "thinkingConfig", Map.of("thinkingBudget", 0)
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    String.format(AI_API_URL_TEMPLATE, model), entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (Exception e) {
            log.error("AI provider call failed for STELLA chatbot: {}", e.getMessage());
            return fallbackReply();
        }
    }

    public Map<String, Object> search(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://api.duckduckgo.com/?q=" + encoded + "&format=json&no_html=1&skip_disambig=1";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("abstract",     root.path("Abstract").asText());
            result.put("abstractText", root.path("AbstractText").asText());
            result.put("abstractUrl",  root.path("AbstractURL").asText());
            result.put("answer",       root.path("Answer").asText());

            List<Map<String, String>> topics = new ArrayList<>();
            JsonNode relatedTopics = root.path("RelatedTopics");
            if (relatedTopics.isArray()) {
                for (int i = 0; i < Math.min(relatedTopics.size(), 5); i++) {
                    JsonNode t = relatedTopics.get(i);
                    if (t.has("Text") && t.has("FirstURL")) {
                        Map<String, String> topic = new LinkedHashMap<>();
                        topic.put("text", t.path("Text").asText());
                        topic.put("url",  t.path("FirstURL").asText());
                        topics.add(topic);
                    }
                }
            }
            result.put("relatedTopics", topics);
            return result;
        } catch (Exception e) {
            log.error("DuckDuckGo search failed for query '{}': {}", query, e.getMessage());
            return Map.of("abstract", "", "relatedTopics", List.of());
        }
    }

    private String fallbackReply() {
        return "Je suis **STELLA**, votre assistante IA SkillSet. Je rencontre une difficulté temporaire. "
             + "Veuillez réessayer dans quelques instants ou utiliser directement les fonctionnalités de la plateforme.";
    }
}
