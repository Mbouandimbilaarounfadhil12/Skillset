package com.skillset.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.skillset.domain.entity.*;
import com.skillset.domain.entity.ConsentRecord.ConsentType;
import com.skillset.domain.entity.GdprRequest.RequestStatus;
import com.skillset.domain.entity.GdprRequest.RequestType;
import com.skillset.domain.entity.UserRole;
import com.skillset.infrastructure.persistence.*;
import com.skillset.infrastructure.util.EmailUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GdprService {

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final InterviewRepository interviewRepository;
    private final ReviewRepository reviewRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final ConsentRecordRepository consentRecordRepository;
    private final GdprRequestRepository gdprRequestRepository;
    private final JobListingRepository jobListingRepository;
    private final EmailUtil emailUtil;

    // ─── EXPORT ──────────────────────────────────────────────────────────────

    public void exportUserData(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Map<String, Object> exportData = new LinkedHashMap<>();
        exportData.put("profil", buildUserProfile(user));
        exportData.put("candidatures", applicationRepository.findByJobSeekerId(userId));
        exportData.put("messages_envoyes", messageRepository.findBySender_Id(userId));
        exportData.put("messages_recus", messageRepository.findByRecipient_Id(userId));
        exportData.put("entretiens", interviewRepository.findByCandidateId(userId));
        exportData.put("preferences", userPreferencesRepository.findByUserId(userId).orElse(null));
        exportData.put("consentements", consentRecordRepository.findByUserId(userId));

        GdprRequest request = new GdprRequest();
        request.setUserId(userId);
        request.setRequestType(RequestType.EXPORT);
        request.setStatus(RequestStatus.COMPLETED);
        request.setRequestedAt(LocalDateTime.now());
        request.setCompletedAt(LocalDateTime.now());
        gdprRequestRepository.save(request);

        emailUtil.sendDataExportConfirmation(user.getEmail(), user.getFirstName());
    }

    private Map<String, Object> buildUserProfile(User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("prenom", user.getFirstName());
        profile.put("nom", user.getLastName());
        profile.put("telephone", user.getPhoneNumber());
        profile.put("role", user.getRole());
        profile.put("inscritLe", user.getCreatedAt());
        return profile;
    }

    // ─── ERASURE ─────────────────────────────────────────────────────────────

    @Transactional
    public void eraseUserData(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Email AVANT toute suppression
        emailUtil.sendAccountDeletionConfirmation(user.getEmail(), user.getFirstName());

        // 1. ApplicationAnswers + Reviews via cascade sur Application (CascadeType.ALL)
        // 2. Applications
        applicationRepository.deleteByJobSeekerId(userId);

        // 3. Entretiens
        interviewRepository.deleteByCandidateId(userId);

        // 4. Messages (envoyés et reçus)
        messageRepository.deleteBySenderIdOrRecipientId(userId);

        // 5. Notifications
        notificationRepository.deleteByUser_Id(userId);

        // 6. ConsentRecords
        consentRecordRepository.deleteByUserId(userId);

        // 7. GdprRequests
        gdprRequestRepository.deleteByUserId(userId);

        // 8. Profil candidat ou employeur
        if (user.getRole() == UserRole.CANDIDATE) {
            candidateProfileRepository.deleteByUserId(userId);
        } else if (user.getRole() == UserRole.EMPLOYER) {
            employerProfileRepository.deleteByUserId(userId);
            jobListingRepository.clearCompanyIdByUserId(userId);
        }

        // 9. Préférences
        userPreferencesRepository.deleteByUserId(userId);

        // 10. Cloudinary — photo de profil
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isBlank()) {
            deleteFromCloudinary(user.getProfilePictureUrl());
        }

        // 11. Suppression du compte
        userRepository.deleteById(userId);
    }

    private void deleteFromCloudinary(String url) {
        try {
            // Extraire le publicId depuis l'URL Cloudinary
            String[] parts = url.split("/");
            if (parts.length > 0) {
                String filename = parts[parts.length - 1];
                String publicId = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;
                com.cloudinary.Cloudinary cloudinary = new com.cloudinary.Cloudinary();
                cloudinary.uploader().destroy(publicId, com.cloudinary.utils.ObjectUtils.emptyMap());
            }
        } catch (Exception e) {
            // Log mais ne pas bloquer la suppression
        }
    }

    // ─── CONSENTS ────────────────────────────────────────────────────────────

    public void updateConsent(String userId, ConsentType consentType, boolean accepted,
                               HttpServletRequest request) {
        ConsentRecord record = consentRecordRepository
                .findByUserIdAndConsentType(userId, consentType)
                .orElseGet(() -> {
                    ConsentRecord r = new ConsentRecord();
                    r.setUserId(userId);
                    r.setConsentType(consentType);
                    return r;
                });

        record.setAccepted(accepted);
        record.setIpAddress(request.getRemoteAddr());
        record.setUserAgent(request.getHeader("User-Agent"));
        record.setConsentedAt(LocalDateTime.now());
        record.setRevokedAt(accepted ? null : LocalDateTime.now());

        consentRecordRepository.save(record);
    }

    public List<ConsentRecord> getUserConsents(String userId) {
        return consentRecordRepository.findByUserIdAndRevokedAtIsNull(userId);
    }

    public void revokeAllConsents(String userId) {
        List<ConsentRecord> consents = consentRecordRepository.findByUserIdAndRevokedAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        consents.forEach(c -> c.setRevokedAt(now));
        consentRecordRepository.saveAll(consents);
    }
}
