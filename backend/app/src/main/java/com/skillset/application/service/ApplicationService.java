package com.skillset.application.service;

import com.skillset.application.dto.ApplicationDTO;
import com.skillset.application.dto.MatchResult;
import com.skillset.domain.entity.Application;
import com.skillset.domain.entity.ApplicationStatus;
import com.skillset.domain.entity.JobListing;
import com.skillset.domain.entity.User;
import com.skillset.domain.port.ApplicationRepositoryPort;
import com.skillset.domain.port.JobRepositoryPort;
import com.skillset.domain.port.UserRepositoryPort;
import com.skillset.infrastructure.security.AuthorizationService;
import com.skillset.infrastructure.util.CVParserUtil;
import com.skillset.infrastructure.util.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepositoryPort applicationRepositoryPort;
    private final JobRepositoryPort          jobRepositoryPort;
    private final UserRepositoryPort         userRepositoryPort;
    private final AuthorizationService       authorizationService;
    private final CVParserUtil               cvParserUtil;
    private final CloudinaryService          cloudinaryService;
    private final AiCompletionService          aiCompletionService;
    private final NotificationPushService    notificationPushService;

    // ── Soumission ─────────────────────────────────────────────────────────────

    public Application submitApplication(String candidateId,
                                         String jobListingId,
                                         String coverLetter,
                                         MultipartFile cvFile) {
        // 1. Extraction texte PDF + upload Cloudinary
        String cvText = "";
        String cvUrl  = null;

        if (cvFile != null && !cvFile.isEmpty()) {
            try {
                byte[] bytes = cvFile.getBytes();
                cvText = cvParserUtil.extractTextFromPdf(bytes);
                cvUrl  = cloudinaryService.uploadCv(cvFile);
                log.info("CV uploadé pour {} — {} chars extraits", candidateId, cvText.length());
            } catch (IOException e) {
                log.warn("Erreur de lecture du fichier CV : {}", e.getMessage());
            }
        }

        // 2. Construction de l'entité
        Application application = new Application();
        application.setJobSeekerId(candidateId);
        application.setCoverLetter(coverLetter);
        application.setCvUrl(cvUrl);
        application.setStatus(ApplicationStatus.SUBMITTED);

        JobListing ref = new JobListing();
        ref.setId(jobListingId);
        application.setJobListing(ref);

        // 3. Match score — IA en priorité, sinon keyword-overlap
        String fullCvText = nvl(cvText) + " " + nvl(coverLetter);
        jobRepositoryPort.findById(jobListingId).ifPresent(job -> {
            String jobText = nvl(job.getRequiredSkills()) + " " + nvl(job.getDescription());

            Optional<MatchResult> aiResult = aiCompletionService.analyzeMatch(fullCvText, jobText);
            if (aiResult.isPresent()) {
                application.setMatchScore(aiResult.get().score());
                application.setMatchExplanation(aiResult.get().explanation());
            } else {
                application.setMatchScore(cvParserUtil.calculateMatchScore(fullCvText, jobText));
            }
        });

        return applicationRepositoryPort.save(application);
    }

    // ── Lecture candidat ───────────────────────────────────────────────────────

    public List<ApplicationDTO> getCandidateApplications(String currentUserId, String jobSeekerId) {
        authorizationService.requireSelfOrAdmin(currentUserId, jobSeekerId);
        return applicationRepositoryPort.findByJobSeekerId(jobSeekerId)
                .stream().map(this::toDTO).toList();
    }

    // ── Lecture recruteur : toutes les candidatures de ses offres ──────────────

    public List<ApplicationDTO> getEmployerApplications(String employerId) {
        return jobRepositoryPort.findByCompanyId(employerId).stream()
                .flatMap(job -> applicationRepositoryPort.findByJobListingId(job.getId()).stream())
                .map(this::toDTO)
                .toList();
    }

    public List<ApplicationDTO> getJobApplications(String currentUserId, String jobListingId) {
        authorizationService.requireJobOwner(currentUserId, jobListingId);
        return applicationRepositoryPort.findByJobListingId(jobListingId)
                .stream().map(this::toDTO).toList();
    }

    public Optional<Application> getApplicationById(String currentUserId, String applicationId) {
        Optional<Application> application = applicationRepositoryPort.findById(applicationId);
        application.ifPresent(app -> authorizationService.requireApplicationAccess(currentUserId, app));
        return application;
    }

    // ── Mise à jour statut ─────────────────────────────────────────────────────

    public ApplicationDTO updateStatus(String currentUserId, String applicationId, String newStatus) {
        Application app = applicationRepositoryPort.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidature introuvable"));
        authorizationService.requireApplicationAccess(currentUserId, app);
        try {
            app.setStatus(ApplicationStatus.valueOf(newStatus.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide : " + newStatus);
        }
        if (app.getStatus() == ApplicationStatus.APPROVED || app.getStatus() == ApplicationStatus.REJECTED) {
            app.setReviewedAt(LocalDateTime.now());
        }
        ApplicationDTO saved = toDTO(applicationRepositoryPort.save(app));

        // Push real-time notification to the candidate
        String jobTitle = saved.jobTitle() != null ? saved.jobTitle() : "votre offre";
        String notifTitle = switch (app.getStatus()) {
            case APPROVED  -> "Candidature acceptée 🎉";
            case REJECTED  -> "Candidature refusée";
            case INTERVIEW -> "Entretien planifié";
            case OFFER     -> "Offre reçue";
            case SCREENING -> "Candidature en cours d'examen";
            default        -> "Statut mis à jour";
        };
        String notifBody = switch (app.getStatus()) {
            case APPROVED  -> "Félicitations ! Votre candidature pour « " + jobTitle + " » a été acceptée.";
            case REJECTED  -> "Votre candidature pour « " + jobTitle + " » n'a pas été retenue.";
            case INTERVIEW -> "Vous avez été sélectionné(e) pour un entretien — « " + jobTitle + " ».";
            case OFFER     -> "Une offre vous a été soumise pour le poste « " + jobTitle + " ».";
            default        -> "Le statut de votre candidature « " + jobTitle + " » a changé : " + newStatus + ".";
        };
        notificationPushService.push(app.getJobSeekerId(), "application", notifTitle, notifBody, "/applications");

        return saved;
    }

    // ── DTO ────────────────────────────────────────────────────────────────────

    private ApplicationDTO toDTO(Application app) {
        String candidateName  = "";
        String candidateEmail = "";
        String candidatePhone = "";
        Optional<User> candidate = userRepositoryPort.findUserById(app.getJobSeekerId());
        if (candidate.isPresent()) {
            User u = candidate.get();
            candidateName  = nvl(u.getFirstName()) + " " + nvl(u.getLastName());
            candidateEmail = nvl(u.getEmail());
            candidatePhone = nvl(u.getPhoneNumber());
        }

        String jobTitle = "";
        String jobLocation = "";
        String jobType = "";
        String companyId = null;
        String employerName = "";
        String employerPhone = "";
        JobListing job = app.getJobListing() != null
                ? jobRepositoryPort.findById(app.getJobListing().getId()).orElse(null)
                : null;
        if (job != null) {
            jobTitle = nvl(job.getTitle());
            jobLocation = nvl(job.getLocation());
            jobType = nvl(job.getJobType());
            companyId = job.getCompanyId();
            Optional<User> employer = userRepositoryPort.findUserById(job.getCompanyId());
            if (employer.isPresent()) {
                User e = employer.get();
                employerName = (nvl(e.getFirstName()) + " " + nvl(e.getLastName())).trim();
                employerPhone = nvl(e.getPhoneNumber());
            }
        }

        return new ApplicationDTO(
                app.getId(),
                app.getJobSeekerId(),
                app.getJobListing() != null ? app.getJobListing().getId() : null,
                app.getCoverLetter(),
                app.getCvUrl(),
                app.getStatus().toString(),
                app.getMatchScore(),
                app.getMatchExplanation(),
                candidateName.trim(),
                candidateEmail,
                jobTitle,
                candidatePhone,
                companyId,
                employerName,
                employerPhone,
                jobLocation,
                jobType
        );
    }

    private static String nvl(String s) { return s != null ? s : ""; }
}
