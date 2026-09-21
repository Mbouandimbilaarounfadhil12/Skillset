package com.skillset.application.service;

import com.skillset.application.dto.JobListingDTO;
import com.skillset.application.dto.ScoredCandidateDTO;
import com.skillset.application.dto.ScoredJobDTO;
import com.skillset.domain.entity.CandidateProfile;
import com.skillset.domain.entity.EmployerProfile;
import com.skillset.domain.entity.JobListing;
import com.skillset.domain.entity.JobStatus;
import com.skillset.domain.port.ApplicationRepositoryPort;
import com.skillset.domain.port.EmployerProfileRepositoryPort;
import com.skillset.domain.port.JobRepositoryPort;
import com.skillset.infrastructure.persistence.CandidateProfileRepository;
import com.skillset.infrastructure.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepositoryPort jobRepositoryPort;
    private final AuthorizationService authorizationService;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepositoryPort employerProfileRepositoryPort;
    private final ApplicationRepositoryPort applicationRepositoryPort;
    private final ScoringService scoringService;

    public JobListing createJob(String employerId, JobListing jobListing) {
        jobListing.setCompanyId(employerId);
        return jobRepositoryPort.save(jobListing);
    }

    public List<JobListingDTO> getAllJobs() {
        return jobRepositoryPort.findAll()
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Recherche/filtrage réel des offres ouvertes, avec tri personnalisé par
     * score de matching quand l'utilisateur connecté est un candidat ayant un
     * profil (issu de l'onboarding) — chaque utilisateur voit donc un
     * classement d'offres qui lui est propre plutôt qu'une liste identique.
     */
    public List<JobListingDTO> searchJobs(String currentUserId, String q, String location, String type,
                                           String sector, Integer salaryMin, Boolean remote, String sort) {
        List<JobListing> jobs = jobRepositoryPort.findAll().stream()
            .filter(job -> job.getStatus() == JobStatus.OPEN)
            .filter(job -> matchesKeywords(job, q))
            .filter(job -> matchesLocation(job, location))
            .filter(job -> matchesType(job, type))
            .filter(job -> matchesSector(job, sector))
            .filter(job -> matchesSalaryMin(job, salaryMin))
            .filter(job -> matchesRemote(job, remote))
            .toList();

        Optional<CandidateProfile> candidateOpt = currentUserId != null
            ? candidateProfileRepository.findByUserId(currentUserId)
            : Optional.empty();

        boolean explicitSort = sort != null && !sort.isBlank() && !"relevance".equalsIgnoreCase(sort);

        if (!explicitSort && candidateOpt.isPresent()) {
            List<ScoredJobDTO> scored = scoringService.scoreJobsForCandidate(candidateOpt.get().getId(), jobs);
            Map<String, JobListing> byId = jobs.stream()
                .collect(Collectors.toMap(JobListing::getId, j -> j, (a, b) -> a, LinkedHashMap::new));
            return scored.stream()
                .filter(s -> byId.containsKey(s.id()))
                .map(s -> convertToDTO(byId.get(s.id()), s.score()))
                .toList();
        }

        Comparator<JobListing> comparator = switch (sort == null ? "" : sort.toLowerCase()) {
            case "salary" -> Comparator.comparing((JobListing j) -> parseIntSafe(j.getSalaryMax())).reversed();
            default -> Comparator.comparing(JobListing::getPostedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        };

        return jobs.stream()
            .sorted(comparator)
            .map(this::convertToDTO)
            .toList();
    }

    private boolean matchesKeywords(JobListing job, String q) {
        if (q == null || q.isBlank()) return true;
        String needle = q.trim().toLowerCase();
        return containsIgnoreCase(job.getTitle(), needle)
            || containsIgnoreCase(job.getDescription(), needle)
            || containsIgnoreCase(job.getRequiredSkills(), needle);
    }

    private boolean matchesLocation(JobListing job, String location) {
        if (location == null || location.isBlank()) return true;
        return containsIgnoreCase(job.getLocation(), location.trim());
    }

    private boolean matchesType(JobListing job, String type) {
        if (type == null || type.isBlank()) return true;
        return job.getJobType() != null && job.getJobType().equalsIgnoreCase(type.trim());
    }

    private boolean matchesSector(JobListing job, String sector) {
        if (sector == null || sector.isBlank()) return true;
        Optional<EmployerProfile> employer = employerProfileRepositoryPort.findByUserId(job.getCompanyId());
        return employer.map(EmployerProfile::getIndustry)
            .map(industry -> containsIgnoreCase(industry, sector.trim()))
            .orElse(false);
    }

    private boolean matchesSalaryMin(JobListing job, Integer salaryMin) {
        if (salaryMin == null) return true;
        Integer jobMax = parseIntSafe(job.getSalaryMax());
        return jobMax == null || jobMax >= salaryMin;
    }

    private boolean matchesRemote(JobListing job, Boolean remote) {
        if (remote == null || !remote) return true;
        return isRemote(job);
    }

    private boolean isRemote(JobListing job) {
        String haystack = ((nvl(job.getLocation())) + " " + nvl(job.getJobType()) + " " + nvl(job.getDescription())).toLowerCase();
        return haystack.contains("remote") || haystack.contains("télétravail") || haystack.contains("teletravail");
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle.toLowerCase());
    }

    private Integer parseIntSafe(String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    public List<JobListingDTO> getJobsByLocation(String location) {
        return jobRepositoryPort.findByLocation(location)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<JobListingDTO> getCompanyJobs(String currentUserId, String companyId) {
        authorizationService.requireSelfOrAdmin(currentUserId, companyId);
        return jobRepositoryPort.findByCompanyId(companyId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public Optional<JobListing> getJobById(String jobId) {
        return jobRepositoryPort.findById(jobId);
    }

    public JobListing updateJob(String currentUserId, String jobId, JobListing jobDetails) {
        authorizationService.requireJobOwner(currentUserId, jobId);
        Optional<JobListing> job = jobRepositoryPort.findById(jobId);
        if (job.isPresent()) {
            JobListing existing = job.get();
            existing.setTitle(jobDetails.getTitle());
            existing.setDescription(jobDetails.getDescription());
            existing.setLocation(jobDetails.getLocation());
            existing.setJobType(jobDetails.getJobType());
            existing.setSalaryMin(jobDetails.getSalaryMin());
            existing.setSalaryMax(jobDetails.getSalaryMax());
            return jobRepositoryPort.save(existing);
        }
        return null;
    }

    public List<ScoredCandidateDTO> getSuggestedCandidates(String userId, String jobId) {
        authorizationService.requireJobOwner(userId, jobId);
        List<CandidateProfile> allCandidates = candidateProfileRepository.findAll();
        return scoringService.scoreCandidatesForJob(jobId, allCandidates)
            .stream()
            .limit(10)
            .toList();
    }

    public List<ScoredJobDTO> getSuggestedJobs(String userId) {
        Optional<CandidateProfile> candidateOpt = candidateProfileRepository.findByUserId(userId);
        if (candidateOpt.isEmpty()) {
            return Collections.emptyList();
        }

        List<JobListing> openJobs = jobRepositoryPort.findAll()
            .stream()
            .filter(job -> job.getStatus() == JobStatus.OPEN)
            .toList();

        return scoringService.scoreJobsForCandidate(candidateOpt.get().getId(), openJobs)
            .stream()
            .limit(10)
            .toList();
    }

    private JobListingDTO convertToDTO(JobListing job) {
        return convertToDTO(job, null);
    }

    private JobListingDTO convertToDTO(JobListing job, Double matchScore) {
        Optional<EmployerProfile> employer = employerProfileRepositoryPort.findByUserId(job.getCompanyId());

        String company = employer.map(EmployerProfile::getCompanyName).orElse(job.getCompanyId());
        String sector = employer.map(EmployerProfile::getIndustry).orElse(null);
        String currency = employer.map(EmployerProfile::getCompanyCountry)
            .filter(country -> country.equalsIgnoreCase("France"))
            .map(country -> "EUR")
            .orElse("FCFA");

        List<String> skills = job.getRequiredSkills() == null || job.getRequiredSkills().isBlank()
            ? Collections.emptyList()
            : Arrays.stream(job.getRequiredSkills().split(",")).map(String::trim).toList();

        int postedDaysAgo = job.getPostedAt() != null
            ? (int) Duration.between(job.getPostedAt(), LocalDateTime.now()).toDays()
            : 0;
        int applicants = applicationRepositoryPort.findByJobListingId(job.getId()).size();

        return new JobListingDTO(
            job.getId(),
            job.getTitle(),
            job.getDescription(),
            job.getCompanyId(),
            job.getLocation(),
            job.getJobType(),
            job.getSalaryMin(),
            job.getSalaryMax(),
            job.getRequiredSkills(),
            job.getResponsibilities(),
            job.getStatus().toString(),
            company,
            sector,
            currency,
            skills,
            isRemote(job),
            postedDaysAgo,
            applicants,
            postedDaysAgo <= 2,
            matchScore
        );
    }
}
