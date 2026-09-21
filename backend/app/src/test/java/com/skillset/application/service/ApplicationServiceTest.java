package com.skillset.application.service;

import com.skillset.application.dto.ApplicationDTO;
import com.skillset.application.dto.MatchResult;
import com.skillset.application.service.AiCompletionService;
import com.skillset.domain.entity.Application;
import com.skillset.domain.entity.ApplicationStatus;
import com.skillset.domain.entity.JobListing;
import com.skillset.domain.entity.User;
import com.skillset.domain.entity.UserRole;
import com.skillset.domain.port.ApplicationRepositoryPort;
import com.skillset.domain.port.JobRepositoryPort;
import com.skillset.domain.port.UserRepositoryPort;
import com.skillset.infrastructure.security.AuthorizationService;
import com.skillset.infrastructure.util.CVParserUtil;
import com.skillset.infrastructure.util.CloudinaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationService")
class ApplicationServiceTest {

    @Mock ApplicationRepositoryPort applicationRepositoryPort;
    @Mock JobRepositoryPort          jobRepositoryPort;
    @Mock UserRepositoryPort         userRepositoryPort;
    @Mock AuthorizationService       authorizationService;
    @Mock CVParserUtil               cvParserUtil;
    @Mock CloudinaryService          cloudinaryService;
    @Mock AiCompletionService         aiCompletionService;
    @Mock NotificationPushService    notificationPushService;

    @InjectMocks ApplicationService applicationService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User candidateUser() {
        User u = new User();
        u.setId("candidate-1");
        u.setEmail("alice@example.com");
        u.setFirstName("Alice");
        u.setLastName("Dupont");
        u.setRole(UserRole.CANDIDATE);
        u.setIsActive(true);
        return u;
    }

    private JobListing jobListing() {
        JobListing j = new JobListing();
        j.setId("job-1");
        j.setTitle("Développeur Java");
        j.setDescription("Spring Boot, PostgreSQL");
        j.setRequiredSkills("Java Spring PostgreSQL");
        return j;
    }

    private Application savedApplication() {
        Application app = new Application();
        app.setId("app-1");
        app.setJobSeekerId("candidate-1");
        app.setJobListing(jobListing());
        app.setCoverLetter("Expérience Java Spring Boot");
        app.setMatchScore(75.0);
        app.setMatchExplanation("Bon profil Java Spring.");
        app.setStatus(ApplicationStatus.SUBMITTED);
        return app;
    }

    // ── submitApplication() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("submitApplication()")
    class SubmitApplication {

        @Test
        @DisplayName("utilise l'IA pour scorer quand la clé est configurée")
        void withCvFile_usesAiScore() throws IOException {
            MultipartFile cv = mock(MultipartFile.class);
            when(cv.isEmpty()).thenReturn(false);
            when(cv.getBytes()).thenReturn("java spring developer".getBytes());

            when(cvParserUtil.extractTextFromPdf(any(byte[].class))).thenReturn("Java Spring Developer");
            when(cloudinaryService.uploadCv(cv)).thenReturn("https://res.cloudinary.com/test/cv.pdf");
            when(jobRepositoryPort.findById("job-1")).thenReturn(Optional.of(jobListing()));
            when(aiCompletionService.analyzeMatch(anyString(), anyString()))
                    .thenReturn(Optional.of(new MatchResult(87.0, "Excellent profil Java Spring.")));
            when(applicationRepositoryPort.save(any(Application.class))).thenReturn(savedApplication());

            applicationService.submitApplication("candidate-1", "job-1", "Lettre de motivation", cv);

            verify(aiCompletionService).analyzeMatch(anyString(), anyString());
            verify(cvParserUtil, never()).calculateMatchScore(anyString(), anyString());
        }

        @Test
        @DisplayName("utilise keyword-overlap si l'IA n'est pas disponible")
        void aiAbsent_fallsBackToKeywordScore() {
            when(jobRepositoryPort.findById("job-1")).thenReturn(Optional.of(jobListing()));
            when(aiCompletionService.analyzeMatch(anyString(), anyString())).thenReturn(Optional.empty());
            when(cvParserUtil.calculateMatchScore(anyString(), anyString())).thenReturn(50.0);
            when(applicationRepositoryPort.save(any(Application.class))).thenReturn(savedApplication());

            applicationService.submitApplication("candidate-1", "job-1", "Lettre", null);

            verify(cvParserUtil).calculateMatchScore(anyString(), anyString());
        }

        @Test
        @DisplayName("sauvegarde sans PDF si aucun fichier n'est fourni")
        void withoutCvFile_savesWithoutParsing() {
            when(jobRepositoryPort.findById("job-1")).thenReturn(Optional.of(jobListing()));
            when(aiCompletionService.analyzeMatch(anyString(), anyString())).thenReturn(Optional.empty());
            when(cvParserUtil.calculateMatchScore(anyString(), anyString())).thenReturn(0.0);
            when(applicationRepositoryPort.save(any(Application.class))).thenReturn(savedApplication());

            applicationService.submitApplication("candidate-1", "job-1", "Lettre", null);

            verify(cvParserUtil, never()).extractTextFromPdf(any());
            verify(cloudinaryService, never()).uploadCv(any());
        }

        @Test
        @DisplayName("lettre de motivation incluse dans le texte soumis à l'IA")
        void coverLetterIncludedInAiCall() {
            when(jobRepositoryPort.findById("job-1")).thenReturn(Optional.of(jobListing()));
            when(aiCompletionService.analyzeMatch(anyString(), anyString())).thenReturn(Optional.empty());
            when(cvParserUtil.calculateMatchScore(anyString(), anyString())).thenReturn(50.0);
            when(applicationRepositoryPort.save(any(Application.class))).thenReturn(savedApplication());

            applicationService.submitApplication("candidate-1", "job-1", "Java Spring expert", null);

            verify(aiCompletionService).analyzeMatch(contains("Java Spring expert"), anyString());
        }
    }

    // ── getCandidateApplications() ─────────────────────────────────────────────

    @Nested
    @DisplayName("getCandidateApplications()")
    class GetCandidateApplications {

        @Test
        @DisplayName("retourne la liste des candidatures enrichies en DTO")
        void returnsEnrichedDtos() {
            Application app = savedApplication();
            doNothing().when(authorizationService).requireSelfOrAdmin("candidate-1", "candidate-1");
            when(applicationRepositoryPort.findByJobSeekerId("candidate-1")).thenReturn(List.of(app));
            when(userRepositoryPort.findUserById("candidate-1")).thenReturn(Optional.of(candidateUser()));

            List<ApplicationDTO> result = applicationService.getCandidateApplications("candidate-1", "candidate-1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).candidateEmail()).isEqualTo("alice@example.com");
            assertThat(result.get(0).jobTitle()).isEqualTo("Développeur Java");
        }

        @Test
        @DisplayName("retourne une liste vide s'il n'y a aucune candidature")
        void noApplications_returnsEmptyList() {
            doNothing().when(authorizationService).requireSelfOrAdmin("candidate-1", "candidate-1");
            when(applicationRepositoryPort.findByJobSeekerId("candidate-1")).thenReturn(List.of());

            assertThat(applicationService.getCandidateApplications("candidate-1", "candidate-1")).isEmpty();
        }
    }

    // ── getJobApplications() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getJobApplications()")
    class GetJobApplications {

        @Test
        @DisplayName("retourne les candidatures pour un poste donné")
        void returnsApplicationsForJob() {
            Application app = savedApplication();
            doNothing().when(authorizationService).requireJobOwner("employer-1", "job-1");
            when(applicationRepositoryPort.findByJobListingId("job-1")).thenReturn(List.of(app));
            when(userRepositoryPort.findUserById("candidate-1")).thenReturn(Optional.of(candidateUser()));

            List<ApplicationDTO> result = applicationService.getJobApplications("employer-1", "job-1");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).jobListingId()).isEqualTo("job-1");
        }
    }

    // ── updateStatus() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("met à jour le statut d'une candidature existante")
        void validStatus_updatesApplication() {
            Application app = savedApplication();
            when(applicationRepositoryPort.findById("app-1")).thenReturn(Optional.of(app));
            doNothing().when(authorizationService).requireApplicationAccess("candidate-1", app);
            when(applicationRepositoryPort.save(app)).thenReturn(app);
            when(userRepositoryPort.findUserById("candidate-1")).thenReturn(Optional.of(candidateUser()));
            doNothing().when(notificationPushService).push(anyString(), anyString(), anyString(), anyString(), anyString());

            ApplicationDTO result = applicationService.updateStatus("candidate-1", "app-1", "INTERVIEW");

            assertThat(result.status()).isEqualTo("INTERVIEW");
        }

        @Test
        @DisplayName("lève NOT_FOUND si la candidature n'existe pas")
        void notFound_throwsNotFound() {
            when(applicationRepositoryPort.findById("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.updateStatus("candidate-1", "unknown", "INTERVIEW"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("lève BAD_REQUEST pour un statut invalide")
        void invalidStatus_throwsBadRequest() {
            Application app = savedApplication();
            when(applicationRepositoryPort.findById("app-1")).thenReturn(Optional.of(app));
            doNothing().when(authorizationService).requireApplicationAccess("candidate-1", app);

            assertThatThrownBy(() -> applicationService.updateStatus("candidate-1", "app-1", "INVALIDE"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
