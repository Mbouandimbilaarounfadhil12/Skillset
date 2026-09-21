package com.skillset.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillset.application.dto.AuthResponse;
import com.skillset.application.dto.onboarding.CompleteOnboardingRequest;
import com.skillset.application.dto.onboarding.GenerateQuestionsRequest;
import com.skillset.application.dto.onboarding.GenerateQuestionsResponse;
import com.skillset.application.dto.onboarding.QuestionDto;
import com.skillset.domain.entity.CandidateProfile;
import com.skillset.domain.entity.EmployerProfile;
import com.skillset.domain.entity.User;
import com.skillset.domain.port.CandidateProfileRepositoryPort;
import com.skillset.domain.port.EmployerProfileRepositoryPort;
import com.skillset.domain.port.UserRepositoryPort;
import com.skillset.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final AiCompletionService aiCompletionService;
    private final UserRepositoryPort userRepositoryPort;
    private final CandidateProfileRepositoryPort candidateProfileRepositoryPort;
    private final EmployerProfileRepositoryPort employerProfileRepositoryPort;
    private final JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GenerateQuestionsResponse generateQuestions(GenerateQuestionsRequest request) {
        List<QuestionDto> questions = "EMPLOYER".equalsIgnoreCase(request.getRole())
                ? aiCompletionService.generateEmployerQuestions(request.getInitialAnswers())
                : aiCompletionService.generateCandidateQuestions(request.getInitialAnswers());
        return new GenerateQuestionsResponse(questions);
    }

    public AuthResponse completeOnboarding(String userId, CompleteOnboardingRequest request) {
        User user = userRepositoryPort.findUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));

        Map<String, String> init = request.getInitialAnswers();
        String aiAnswersJson = toJson(request.getAiAnswers());

        if ("EMPLOYER".equalsIgnoreCase(request.getRole())) {
            EmployerProfile profile = employerProfileRepositoryPort.findByUserId(userId)
                    .orElse(new EmployerProfile());
            profile.setUserId(userId);
            String companyName = init.getOrDefault("companyName", "");
            profile.setCompanyName(companyName);
            if (profile.getCompanySlug() == null || profile.getCompanySlug().isBlank()) {
                profile.setCompanySlug(toSlug(companyName));
            }
            profile.setIndustry(init.getOrDefault("industry", ""));
            profile.setCompanyCountry(init.getOrDefault("companyCountry", ""));
            profile.setCompanyCity(init.getOrDefault("companyCity", ""));
            profile.setCompanySize(init.getOrDefault("companySize", ""));
            profile.setHiringRole(init.getOrDefault("hiringRole", ""));
            profile.setContractType(init.getOrDefault("contractType", ""));
            profile.setCompanyRegistrationNumber(init.getOrDefault("companyRegistrationNumber", ""));
            profile.setCompanyWebsite(init.getOrDefault("companyWebsite", ""));
            profile.setCompanyLinkedIn(init.getOrDefault("companyLinkedIn", ""));
            profile.setCompanyAddress(init.getOrDefault("companyAddress", ""));
            String locationStr = init.getOrDefault("companyLocation", "");
            if (!locationStr.isBlank() && locationStr.contains(",")) {
                String[] parts = locationStr.split(",", 2);
                try {
                    profile.setCompanyLatitude(Double.parseDouble(parts[0].trim()));
                    profile.setCompanyLongitude(Double.parseDouble(parts[1].trim()));
                } catch (NumberFormatException e) {
                    log.warn("Format de coordonnées invalide : {}", locationStr);
                }
            }
            profile.setAiAnswers(aiAnswersJson);
            employerProfileRepositoryPort.save(profile);
        } else {
            CandidateProfile profile = candidateProfileRepositoryPort.findByUserId(userId)
                    .orElse(new CandidateProfile());
            profile.setUserId(userId);
            profile.setJobDomain(init.getOrDefault("domain", ""));
            profile.setDesiredRole(init.getOrDefault("desiredRole", ""));
            profile.setExperienceLevel(init.getOrDefault("experienceLevel", ""));
            profile.setContractType(init.getOrDefault("contractType", ""));
            profile.setCountry(init.getOrDefault("candidateCountry", ""));
            profile.setCity(init.getOrDefault("candidateCity", ""));
            // Merge initial answers + AI answers so all data is persisted (LinkedIn, work auth, etc.)
            Map<String, String> allAnswers = new java.util.HashMap<>(init);
            if (request.getAiAnswers() != null) allAnswers.putAll(request.getAiAnswers());
            profile.setAiAnswers(toJson(allAnswers));
            profile.setWantsCv(request.isWantsCv());
            candidateProfileRepositoryPort.save(profile);
        }

        user.setOnboardingCompleted(true);
        User saved = userRepositoryPort.saveUser(user);
        String token = jwtUtil.generateToken(saved.getId(), saved.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .id(saved.getId())
                .email(saved.getEmail())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .role(saved.getRole().toString())
                .onboardingCompleted(true)
                .build();
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    static String toSlug(String name) {
        if (name == null || name.isBlank()) return "company";
        return name.trim().toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-{2,}", "-");
    }
}
