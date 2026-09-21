package com.skillset.application.service;

import com.skillset.application.dto.ScoredCandidateDTO;
import com.skillset.application.dto.ScoredJobDTO;
import com.skillset.domain.entity.CandidateProfile;
import com.skillset.domain.entity.EmployerProfile;
import com.skillset.domain.entity.JobListing;
import com.skillset.domain.port.EmployerProfileRepositoryPort;
import com.skillset.infrastructure.persistence.CandidateProfileRepository;
import com.skillset.infrastructure.persistence.JobListingRepository;
import com.skillset.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoringService {

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final JobListingRepository jobListingRepository;
    private final EmployerProfileRepositoryPort employerProfileRepositoryPort;

    /**
     * Calculate the match score between a job and a candidate.
     * Returns a breakdown of the score across different criteria.
     *
     * @param job the job listing
     * @param candidate the candidate profile
     * @return ScoreBreakdown with individual component scores
     */
    public ScoredJobDTO.ScoreBreakdown calculateMatchScore(JobListing job, CandidateProfile candidate) {
        double skillsScore = calculateSkillsScore(job, candidate);
        double experienceScore = calculateExperienceScore(job, candidate);
        double locationScore = calculateLocationScore(job, candidate);
        double availabilityScore = calculateAvailabilityScore(candidate);
        double titleScore = calculateTitleScore(job, candidate);
        double domainScore = calculateDomainScore(job, candidate);

        return new ScoredJobDTO.ScoreBreakdown(
            skillsScore,
            experienceScore,
            locationScore,
            availabilityScore,
            titleScore,
            domainScore
        );
    }

    /**
     * Score all jobs for a specific candidate.
     *
     * @param candidateId the candidate ID
     * @param jobs the list of jobs to score
     * @return List of ScoredJobDTO sorted by score descending
     */
    public List<ScoredJobDTO> scoreJobsForCandidate(String candidateId, List<JobListing> jobs) {
        Optional<CandidateProfile> candidateOpt = candidateProfileRepository.findById(candidateId);
        if (candidateOpt.isEmpty()) {
            return Collections.emptyList();
        }

        CandidateProfile candidate = candidateOpt.get();

        return jobs.stream()
            .map(job -> {
                ScoredJobDTO.ScoreBreakdown breakdown = calculateMatchScore(job, candidate);
                double totalScore = calculateTotalScore(breakdown);

                return new ScoredJobDTO(
                    job.getId(),
                    job.getTitle(),
                    job.getCompanyId(),
                    job.getLocation(),
                    job.getJobType(),
                    job.getSalaryMin(),
                    job.getSalaryMax(),
                    job.getRequiredSkills(),
                    job.getDescription(),
                    job.getPostedAt(),
                    job.getExpiresAt(),
                    totalScore,
                    breakdown
                );
            })
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .collect(Collectors.toList());
    }

    /**
     * Score all candidates for a specific job.
     *
     * @param jobId the job ID
     * @param candidates the list of candidates to score
     * @return List of ScoredCandidateDTO sorted by score descending
     */
    public List<ScoredCandidateDTO> scoreCandidatesForJob(String jobId, List<CandidateProfile> candidates) {
        Optional<JobListing> jobOpt = jobListingRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return Collections.emptyList();
        }

        JobListing job = jobOpt.get();

        return candidates.stream()
            .map(candidate -> {
                ScoredJobDTO.ScoreBreakdown breakdown = calculateMatchScore(job, candidate);
                double totalScore = calculateTotalScore(breakdown);

                return new ScoredCandidateDTO(
                    candidate.getId(),
                    candidate.getUserId(),
                    candidate.getJobDomain(),
                    candidate.getDesiredRole(),
                    candidate.getExperienceLevel(),
                    candidate.getLocation(),
                    candidate.getCity(),
                    candidate.getCountry(),
                    candidate.getSkills(),
                    totalScore,
                    breakdown
                );
            })
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .collect(Collectors.toList());
    }

    /**
     * Skills Match (0-35 pts):
     * Count intersection of required skills and candidate skills divided by job required skills.
     */
    private double calculateSkillsScore(JobListing job, CandidateProfile candidate) {
        try {
            Set<String> requiredSkills = parseSkills(job.getRequiredSkills());
            Set<String> candidateSkills = parseSkills(candidate.getSkills());

            if (requiredSkills.isEmpty()) {
                return 35.0; // No required skills = full score
            }

            long matchCount = requiredSkills.stream()
                .filter(candidateSkills::contains)
                .count();

            double score = (double) matchCount / requiredSkills.size() * 35.0;
            return Math.min(35.0, score); // Cap at 35
        } catch (Exception e) {
            return 0.0; // Parsing failed = no score
        }
    }

    /**
     * Experience (0-20 pts):
     * If job requires experience: check candidate experience level.
     * If candidate experience >= job requirement: 20 pts.
     * If candidate experience < job requirement: (candidate_years / required_years) * 20.
     */
    private double calculateExperienceScore(JobListing job, CandidateProfile candidate) {
        try {
            Integer requiredYears = extractExperienceYears(job.getDescription());
            Integer candidateYears = extractExperienceYears(candidate.getExperienceLevel());

            // If no requirement specified, full score
            if (requiredYears == null || requiredYears == 0) {
                return 20.0;
            }

            // If candidate has no experience level, no score
            if (candidateYears == null || candidateYears == 0) {
                return 0.0;
            }

            // If candidate meets or exceeds requirement: full score
            if (candidateYears >= requiredYears) {
                return 20.0;
            }

            // Partial score
            double score = (double) candidateYears / requiredYears * 20.0;
            return Math.min(20.0, score); // Cap at 20
        } catch (Exception e) {
            return 0.0; // Parsing failed = no score
        }
    }

    /**
     * Location (0-15 pts):
     * Exact match (same city): 15 pts.
     * Same region/country: 8 pts.
     * Different: 0 pts.
     */
    private double calculateLocationScore(JobListing job, CandidateProfile candidate) {
        try {
            String jobLocation = normalizeLocation(job.getLocation());
            String jobCity = normalizeLocation(job.getLocation());

            String candidateLocation = normalizeLocation(candidate.getLocation());
            String candidateCity = normalizeLocation(candidate.getCity());
            String candidateCountry = normalizeLocation(candidate.getCountry());

            // Exact match on city
            if (!candidateCity.isEmpty() && candidateCity.equalsIgnoreCase(jobCity)) {
                return 15.0;
            }

            // Check country match
            if (!candidateCountry.isEmpty() && jobLocation.contains(candidateCountry)) {
                return 8.0;
            }

            // Try to extract country from job location and match
            if (!candidateCountry.isEmpty() && jobLocation.toLowerCase().contains(candidateCountry.toLowerCase())) {
                return 8.0;
            }

            return 0.0;
        } catch (Exception e) {
            return 0.0; // Parsing failed = no score
        }
    }

    /**
     * Availability (0-10 pts):
     * Available now (date <= today): 10 pts.
     * Available within 30 days: 7 pts.
     * Available > 30 days: 3 pts.
     * No availability date: 7 pts (assume flexible).
     */
    private double calculateAvailabilityScore(CandidateProfile candidate) {
        try {
            String availabilityStr = candidate.getAvailability();

            // No availability date = assume flexible
            if (availabilityStr == null || availabilityStr.trim().isEmpty()) {
                return 7.0;
            }

            LocalDate availabilityDate = parseAvailabilityDate(availabilityStr);
            if (availabilityDate == null) {
                return 7.0; // Parse failed = assume flexible
            }

            LocalDate today = LocalDate.now();

            // Available now or in the past
            if (!availabilityDate.isAfter(today)) {
                return 10.0;
            }

            // Days until availability
            long daysUntilAvailable = java.time.temporal.ChronoUnit.DAYS.between(today, availabilityDate);

            if (daysUntilAvailable <= 30) {
                return 7.0;
            }

            return 3.0;
        } catch (Exception e) {
            return 7.0; // Parsing failed = assume flexible
        }
    }

    /**
     * Title/Keywords (0-5 pts):
     * Check if candidate.desiredRole or job.title match keywords.
     * Exact match: 5 pts.
     * Partial match: 2.5 pts.
     * No match: 0 pts.
     */
    private double calculateTitleScore(JobListing job, CandidateProfile candidate) {
        try {
            String jobTitle = normalizeForComparison(job.getTitle());
            String desiredRole = normalizeForComparison(candidate.getDesiredRole());

            if (jobTitle.isEmpty() || desiredRole.isEmpty()) {
                return 0.0;
            }

            // Exact match
            if (jobTitle.equalsIgnoreCase(desiredRole)) {
                return 5.0;
            }

            // Partial match - check if one contains the other or they share significant words
            if (jobTitle.contains(desiredRole) || desiredRole.contains(jobTitle)) {
                return 2.5;
            }

            // Check for keyword overlap
            String[] jobWords = jobTitle.split("\\s+");
            String[] desiredWords = desiredRole.split("\\s+");

            long matchingWords = Arrays.stream(jobWords)
                .filter(word -> Arrays.asList(desiredWords).contains(word))
                .count();

            if (matchingWords > 0) {
                return 2.5;
            }

            return 0.0;
        } catch (Exception e) {
            return 0.0; // Parsing failed = no score
        }
    }

    /**
     * Domain Match (0-15 pts):
     * Compares the candidate's declared job domain against the employer's industry
     * (resolved via the job's owning EmployerProfile). Missing data on either side is
     * treated as neutral (full score) rather than penalized, since domain isn't always
     * populated during onboarding.
     */
    private double calculateDomainScore(JobListing job, CandidateProfile candidate) {
        try {
            Optional<EmployerProfile> employerOpt = employerProfileRepositoryPort.findByUserId(job.getCompanyId());
            if (employerOpt.isEmpty()) {
                return 15.0;
            }

            String industry = employerOpt.get().getIndustry();
            String jobDomain = candidate.getJobDomain();

            if (industry == null || industry.trim().isEmpty() || jobDomain == null || jobDomain.trim().isEmpty()) {
                return 15.0;
            }

            return normalizeForComparison(industry).equals(normalizeForComparison(jobDomain)) ? 15.0 : 0.0;
        } catch (Exception e) {
            return 15.0;
        }
    }

    /**
     * Calculate total score from breakdown components.
     * Each component has a max value, sum should be 0-100.
     */
    private double calculateTotalScore(ScoredJobDTO.ScoreBreakdown breakdown) {
        double total = breakdown.skillsPoints() +
                breakdown.experiencePoints() +
                breakdown.locationPoints() +
                breakdown.availabilityPoints() +
                breakdown.titlePoints() +
                breakdown.domainPoints();

        // Ensure score is between 0 and 100
        return Math.max(0.0, Math.min(100.0, total));
    }

    /**
     * Parse skills from CSV or JSON format.
     * Handles both "skill1, skill2, skill3" and ["skill1", "skill2"] formats.
     */
    private Set<String> parseSkills(String skillsStr) {
        Set<String> skills = new HashSet<>();

        if (skillsStr == null || skillsStr.trim().isEmpty()) {
            return skills;
        }

        skillsStr = skillsStr.trim();

        // Handle JSON array format: ["skill1", "skill2"]
        if (skillsStr.startsWith("[") && skillsStr.endsWith("]")) {
            skillsStr = skillsStr.substring(1, skillsStr.length() - 1)
                .replace("\"", "")
                .replace("'", "");
        }

        // Split by comma and normalize
        String[] skillArray = skillsStr.split(",");
        for (String skill : skillArray) {
            String normalized = skill.trim().toLowerCase();
            if (!normalized.isEmpty()) {
                skills.add(normalized);
            }
        }

        return skills;
    }

    /**
     * Extract years of experience from text (e.g., "5 ans", "5 years", "5").
     * Returns null if no year value found.
     */
    private Integer extractExperienceYears(String experienceStr) {
        if (experienceStr == null || experienceStr.trim().isEmpty()) {
            return null;
        }

        // Extract all digits
        String digits = experienceStr.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse availability date from various formats (e.g., "2024-12-31", "31/12/2024").
     * Returns null if parsing fails.
     */
    private LocalDate parseAvailabilityDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // Try common date formats
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ISO_LOCAL_DATE, // 2024-12-31
            DateTimeFormatter.ofPattern("dd/MM/yyyy"), // 31/12/2024
            DateTimeFormatter.ofPattern("dd-MM-yyyy"), // 31-12-2024
            DateTimeFormatter.ofPattern("dd.MM.yyyy"), // 31.12.2024
            DateTimeFormatter.ofPattern("yyyy/MM/dd"), // 2024/12/31
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ignored) {
                // Try next format
            }
        }

        return null;
    }

    /**
     * Normalize location string for comparison.
     */
    private String normalizeLocation(String location) {
        if (location == null) {
            return "";
        }
        return location.trim().toLowerCase();
    }

    /**
     * Normalize string for comparison (remove extra spaces, lowercase).
     */
    private String normalizeForComparison(String str) {
        if (str == null) {
            return "";
        }
        return str.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
