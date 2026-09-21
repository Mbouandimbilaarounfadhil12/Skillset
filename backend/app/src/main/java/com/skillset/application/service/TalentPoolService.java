package com.skillset.application.service;

import com.skillset.application.dto.ScoredCandidateDTO;
import com.skillset.application.dto.TalentPoolDTO;
import com.skillset.application.dto.TalentPoolMemberDTO;
import com.skillset.application.dto.TalentPoolSummaryDTO;
import com.skillset.domain.entity.*;
import com.skillset.infrastructure.persistence.*;
import com.skillset.infrastructure.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TalentPoolService {

    private final TalentPoolRepository talentPoolRepository;
    private final TalentPoolMemberRepository talentPoolMemberRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final JobListingRepository jobListingRepository;
    private final ApplicationRepository applicationRepository;
    private final ScoringService scoringService;
    private final EmailUtil emailUtil;
    private final UserRepository userRepository;
    private final EmployerProfileRepository employerProfileRepository;

    /**
     * Create a new talent pool.
     *
     * @param dto the talent pool DTO
     * @param employerId the employer creating the pool
     * @return the created TalentPoolDTO
     */
    public TalentPoolDTO createPool(TalentPoolDTO dto, String employerId) {
        TalentPool pool = new TalentPool();
        pool.setName(dto.name());
        pool.setDescription(dto.description());
        pool.setCreatedBy(employerId);
        pool.setJobListingId(dto.jobListingId());
        pool.setIsPublic(dto.isPublic());
        pool.setCreatedAt(LocalDateTime.now());
        pool.setUpdatedAt(LocalDateTime.now());

        TalentPool saved = talentPoolRepository.save(pool);
        log.info("Talent pool created: {} by employer {}", saved.getId(), employerId);

        return new TalentPoolDTO(
            saved.getId(),
            saved.getName(),
            saved.getDescription(),
            saved.getJobListingId(),
            saved.getIsPublic(),
            saved.getCreatedAt()
        );
    }

    /**
     * Get all pools for an employer with member counts.
     *
     * @param employerId the employer ID
     * @return list of TalentPoolSummaryDTO
     */
    public List<TalentPoolSummaryDTO> getPoolsForEmployer(String employerId) {
        List<TalentPool> pools = talentPoolRepository.findByCreatedBy(employerId);

        return pools.stream()
            .map(pool -> {
                List<TalentPoolMember> members = talentPoolMemberRepository.findByPoolId(pool.getId());

                // Count members by status (excluding REMOVED)
                long activeCount = members.stream()
                    .filter(m -> m.getStatus() == TalentPoolMemberStatus.ACTIVE)
                    .count();
                long contactedCount = members.stream()
                    .filter(m -> m.getStatus() == TalentPoolMemberStatus.CONTACTED)
                    .count();
                long hiredCount = members.stream()
                    .filter(m -> m.getStatus() == TalentPoolMemberStatus.HIRED)
                    .count();

                // Total includes all non-removed members
                int totalMembers = (int) members.stream()
                    .filter(m -> m.getStatus() != TalentPoolMemberStatus.REMOVED)
                    .count();

                return new TalentPoolSummaryDTO(
                    pool.getId(),
                    pool.getName(),
                    totalMembers,
                    (int) activeCount,
                    (int) contactedCount,
                    (int) hiredCount
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Add a candidate to a talent pool.
     *
     * @param poolId the pool ID
     * @param candidateId the candidate ID
     * @param notes optional notes about the candidate
     * @param source the source of the addition
     * @param employerId the employer adding the candidate
     * @throws ResponseStatusException 403 if not pool owner, 400 if candidate already in pool
     */
    public void addCandidate(String poolId, String candidateId, String notes,
                             TalentPoolMemberSource source, String employerId) {
        // Verify pool exists and ownership
        TalentPool pool = talentPoolRepository.findById(poolId)
            .orElseThrow(() -> {
                log.warn("Pool not found: {}", poolId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Talent pool not found");
            });

        if (!pool.getCreatedBy().equals(employerId)) {
            log.warn("Unauthorized access to pool {} by employer {}", poolId, employerId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to modify this pool");
        }

        // Check if candidate already in pool
        if (talentPoolMemberRepository.existsByPoolIdAndCandidateId(poolId, candidateId)) {
            log.warn("Candidate {} already exists in pool {}", candidateId, poolId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidate already exists in this pool");
        }

        // Get candidate profile for name/email
        CandidateProfile candidate = candidateProfileRepository.findById(candidateId)
            .orElseThrow(() -> {
                log.warn("Candidate profile not found: {}", candidateId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate profile not found");
            });

        // Create and save member
        TalentPoolMember member = new TalentPoolMember();
        member.setPoolId(poolId);
        member.setCandidateId(candidateId);
        member.setAddedBy(employerId);
        member.setAddedAt(LocalDateTime.now());
        member.setNotes(notes);
        member.setSource(source);
        member.setStatus(TalentPoolMemberStatus.ACTIVE);

        talentPoolMemberRepository.save(member);
        log.info("Candidate {} added to pool {} by employer {}", candidateId, poolId, employerId);

        // Send email if pool is public
        if (pool.getIsPublic()) {
            try {
                Optional<User> userOpt = userRepository.findById(candidate.getUserId());
                if (userOpt.isPresent()) {
                    User candidateUser = userOpt.get();
                    String firstName = candidateUser.getFirstName();
                    String email = candidateUser.getEmail();

                    // Get employer company name
                    String companyName = getCompanyName(employerId);

                    emailUtil.sendNotification(
                        email,
                        "Vous avez été ajouté(e) à un pool de talents — SkillSet",
                        "Bonjour " + firstName + ",\n\n" +
                        "Vous avez été ajouté(e) au pool de talents « " + pool.getName() + " » de " + companyName + ".\n\n" +
                        "Cette action signifie que vous avez captivé l'attention de ce recruteur.\n" +
                        "Connectez-vous à votre espace SkillSet pour en savoir plus.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe SkillSet"
                    );
                }
            } catch (Exception e) {
                log.error("Error sending email to candidate {}: {}", candidateId, e.getMessage());
            }
        }
    }

    /**
     * Remove a candidate from a talent pool (soft delete).
     *
     * @param poolId the pool ID
     * @param candidateId the candidate ID
     * @param employerId the employer removing the candidate
     * @throws ResponseStatusException 403 if not pool owner
     */
    public void removeCandidate(String poolId, String candidateId, String employerId) {
        // Verify ownership
        TalentPool pool = talentPoolRepository.findById(poolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Talent pool not found"));

        if (!pool.getCreatedBy().equals(employerId)) {
            log.warn("Unauthorized removal attempt for pool {} by employer {}", poolId, employerId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to modify this pool");
        }

        // Find and update member
        List<TalentPoolMember> members = talentPoolMemberRepository.findByPoolId(poolId);
        TalentPoolMember member = members.stream()
            .filter(m -> m.getCandidateId().equals(candidateId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in pool"));

        member.setStatus(TalentPoolMemberStatus.REMOVED);
        talentPoolMemberRepository.save(member);
        log.info("Candidate {} removed from pool {}", candidateId, poolId);
    }

    /**
     * Get members of a talent pool with pagination.
     *
     * @param poolId the pool ID
     * @param employerId the employer requesting the data
     * @param pageable pagination info
     * @return Page of TalentPoolMemberDTO
     * @throws ResponseStatusException 403 if not pool owner
     */
    public Page<TalentPoolMemberDTO> getPoolMembers(String poolId, String employerId, Pageable pageable) {
        // Verify ownership
        TalentPool pool = talentPoolRepository.findById(poolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Talent pool not found"));

        if (!pool.getCreatedBy().equals(employerId)) {
            log.warn("Unauthorized access to pool {} by employer {}", poolId, employerId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view this pool");
        }

        // Get paginated members
        Page<TalentPoolMember> memberPage = talentPoolMemberRepository.findByPoolIdOrderByAddedAtDesc(poolId, pageable);

        // Map to DTOs with candidate information
        return memberPage.map(member -> {
            Optional<CandidateProfile> candidateOpt = candidateProfileRepository.findById(member.getCandidateId());

            String candidateName = "";
            String candidateTitle = "";
            String candidateSkills = "";

            if (candidateOpt.isPresent()) {
                CandidateProfile profile = candidateOpt.get();
                candidateName = getFullName(profile.getUserId());
                candidateTitle = profile.getDesiredRole() != null ? profile.getDesiredRole() : "";
                candidateSkills = profile.getSkills() != null ? profile.getSkills() : "";
            }

            return new TalentPoolMemberDTO(
                member.getId(),
                member.getPoolId(),
                member.getCandidateId(),
                candidateName,
                candidateTitle,
                candidateSkills,
                null, // score not included in pool view
                member.getNotes(),
                member.getStatus(),
                member.getSource(),
                member.getAddedAt()
            );
        });
    }

    /**
     * Update a member's status in the pool.
     *
     * @param poolId the pool ID
     * @param candidateId the candidate ID
     * @param newStatus the new status
     * @param employerId the employer updating the status
     * @throws ResponseStatusException 403 if not pool owner
     */
    public void updateMemberStatus(String poolId, String candidateId,
                                   TalentPoolMemberStatus newStatus, String employerId) {
        // Verify ownership
        TalentPool pool = talentPoolRepository.findById(poolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Talent pool not found"));

        if (!pool.getCreatedBy().equals(employerId)) {
            log.warn("Unauthorized update attempt for pool {} by employer {}", poolId, employerId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to modify this pool");
        }

        // Find and update member
        List<TalentPoolMember> members = talentPoolMemberRepository.findByPoolId(poolId);
        TalentPoolMember member = members.stream()
            .filter(m -> m.getCandidateId().equals(candidateId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in pool"));

        member.setStatus(newStatus);
        talentPoolMemberRepository.save(member);
        log.info("Candidate {} status updated to {} in pool {}", candidateId, newStatus, poolId);
    }

    /**
     * Add a candidate from an application to a talent pool.
     *
     * @param applicationId the application ID
     * @param poolId the pool ID
     * @param employerId the employer adding from application
     */
    public void addFromApplication(String applicationId, String poolId, String employerId) {
        // Find application - use JpaRepository method explicitly
        Optional<Application> appOpt = ((org.springframework.data.jpa.repository.JpaRepository<Application, String>) applicationRepository).findById(applicationId);
        Application application = appOpt
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        String jobSeekerId = application.getJobSeekerId();

        // Get candidate profile to find candidateId
        // Note: jobSeekerId is userId, need to find candidateId via profile lookup
        CandidateProfile candidate = candidateProfileRepository.findByUserId(jobSeekerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate profile not found"));

        // Add candidate to pool
        addCandidate(poolId, candidate.getId(), null, TalentPoolMemberSource.FROM_APPLICATION, employerId);
        log.info("Candidate added to pool {} from application {}", poolId, applicationId);
    }

    /**
     * Get recommended candidates for a talent pool based on job listing.
     *
     * @param poolId the pool ID
     * @param employerId the employer requesting recommendations
     * @return list of ScoredCandidateDTO
     * @throws ResponseStatusException 403 if not pool owner
     */
    public List<ScoredCandidateDTO> getRecommendedCandidates(String poolId, String employerId) {
        // Find pool and verify ownership
        TalentPool pool = talentPoolRepository.findById(poolId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Talent pool not found"));

        if (!pool.getCreatedBy().equals(employerId)) {
            log.warn("Unauthorized access to pool {} by employer {}", poolId, employerId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to access this pool");
        }

        // Return empty if no job listing
        if (pool.getJobListingId() == null || pool.getJobListingId().isEmpty()) {
            log.info("No job listing associated with pool {}", poolId);
            return Collections.emptyList();
        }

        // Fetch job listing
        JobListing job = jobListingRepository.findById(pool.getJobListingId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job listing not found"));

        // Get all candidates
        List<CandidateProfile> allCandidates = candidateProfileRepository.findAll();

        // Score candidates for this job
        List<ScoredCandidateDTO> scoredCandidates = scoringService.scoreCandidatesForJob(
            pool.getJobListingId(),
            allCandidates
        );

        // Filter out candidates already in pool
        List<ScoredCandidateDTO> recommended = scoredCandidates.stream()
            .filter(candidate -> !talentPoolMemberRepository.existsByPoolIdAndCandidateId(poolId, candidate.id()))
            .limit(10)
            .collect(Collectors.toList());

        log.info("Generated {} recommendations for pool {}", recommended.size(), poolId);
        return recommended;
    }

    // ─────────────────────── Helper Methods ──────────────────────

    /**
     * Get company name for an employer.
     */
    private String getCompanyName(String employerId) {
        try {
            Optional<EmployerProfile> profileOpt = employerProfileRepository.findByUserId(employerId);
            if (profileOpt.isPresent()) {
                return profileOpt.get().getCompanyName();
            }
        } catch (Exception e) {
            log.error("Error fetching employer profile for {}: {}", employerId, e.getMessage());
        }
        return "SkillSet";
    }

    /**
     * Get full name for a user.
     */
    private String getFullName(String userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return user.getFirstName() + " " + user.getLastName();
            }
        } catch (Exception e) {
            log.error("Error fetching user {}: {}", userId, e.getMessage());
        }
        return "";
    }
}
