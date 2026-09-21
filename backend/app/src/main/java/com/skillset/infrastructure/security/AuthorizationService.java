package com.skillset.infrastructure.security;

import com.skillset.domain.entity.Application;
import com.skillset.domain.entity.Interview;
import com.skillset.domain.entity.JobListing;
import com.skillset.domain.entity.User;
import com.skillset.domain.entity.UserRole;
import com.skillset.domain.port.ApplicationRepositoryPort;
import com.skillset.domain.port.JobRepositoryPort;
import com.skillset.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final UserRepositoryPort userRepositoryPort;
    private final JobRepositoryPort jobRepositoryPort;
    private final ApplicationRepositoryPort applicationRepositoryPort;

    public void requireSelfOrAdmin(String currentUserId, String targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            return;
        }
        requireAdmin(currentUserId);
    }

    public void requireAdmin(String userId) {
        User user = findUserOrThrow(userId);
        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux administrateurs");
        }
    }

    public void requireRole(String userId, UserRole role) {
        User user = findUserOrThrow(userId);
        if (user.getRole() != role && user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle insuffisant");
        }
    }

    public void requireJobOwner(String userId, String jobId) {
        JobListing job = jobRepositoryPort.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));
        if (job.getCompanyId().equals(userId)) {
            return;
        }
        requireAdmin(userId);
    }

    public void requireApplicationAccess(String userId, String applicationId) {
        Application app = applicationRepositoryPort.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidature introuvable"));
        requireApplicationAccess(userId, app);
    }

    public void requireApplicationAccess(String userId, Application app) {
        if (app.getJobSeekerId().equals(userId)) {
            return;
        }
        if (app.getJobListing() != null && app.getJobListing().getCompanyId().equals(userId)) {
            return;
        }
        requireAdmin(userId);
    }

    public void requireConversationParticipant(String userId, String userId1, String userId2) {
        if (userId.equals(userId1) || userId.equals(userId2)) {
            return;
        }
        requireAdmin(userId);
    }

    public void requireInterviewAccess(String userId, Interview interview) {
        if (userId.equals(interview.getCandidateId()) || userId.equals(interview.getInterviewerId())) {
            return;
        }
        requireAdmin(userId);
    }

    private User findUserOrThrow(String userId) {
        return userRepositoryPort.findUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }
}
