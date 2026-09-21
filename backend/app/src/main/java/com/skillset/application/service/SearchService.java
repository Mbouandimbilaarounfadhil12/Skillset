package com.skillset.application.service;

import com.skillset.application.dto.*;
import com.skillset.domain.entity.*;
import com.skillset.infrastructure.persistence.*;
import com.skillset.infrastructure.persistence.specification.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final JobListingRepository jobListingRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final ScoringService scoringService;

    public Page<ScoredJobDTO> searchJobs(JobSearchCriteria criteria, String currentUserId) {
        // Build Specification from criteria
        List<Specification<JobListing>> specs = new ArrayList<>();
        if (criteria.keywords() != null) specs.add(JobListingSpecification.withKeywords(criteria.keywords()));
        if (criteria.location() != null) specs.add(JobListingSpecification.withLocation(criteria.location()));
        if (criteria.contractType() != null) specs.add(JobListingSpecification.withContractType(criteria.contractType()));
        if (criteria.minSalary() != null || criteria.maxSalary() != null)
            specs.add(JobListingSpecification.withSalaryRange(criteria.minSalary(), criteria.maxSalary()));
        if (criteria.skills() != null && !criteria.skills().isEmpty())
            specs.add(JobListingSpecification.withSkills(criteria.skills()));
        if (criteria.postedWithinDays() != null)
            specs.add(JobListingSpecification.withPostedWithin(criteria.postedWithinDays()));

        Specification<JobListing> combined = JobListingSpecification.combine(specs);

        Pageable pageable = PageRequest.of(criteria.page(), criteria.size());
        Page<JobListing> results = jobListingRepository.findAll(combined, pageable);

        Optional<CandidateProfile> candidateOpt = currentUserId != null
            ? candidateProfileRepository.findByUserId(currentUserId)
            : Optional.empty();

        return results.map(job -> {
            ScoredJobDTO.ScoreBreakdown breakdown = candidateOpt
                .map(candidate -> scoringService.calculateMatchScore(job, candidate))
                .orElse(new ScoredJobDTO.ScoreBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
            Double score = breakdown.skillsPoints() + breakdown.experiencePoints() + breakdown.locationPoints() +
                          breakdown.availabilityPoints() + breakdown.titlePoints() + breakdown.domainPoints();
            return new ScoredJobDTO(job.getId(), job.getTitle(), job.getCompanyId(), job.getLocation(),
                job.getJobType(), job.getSalaryMin(), job.getSalaryMax(), job.getRequiredSkills(),
                job.getDescription(), job.getPostedAt(), job.getExpiresAt(), score, breakdown);
        });
    }

    public Page<ScoredCandidateDTO> searchCandidates(CandidateSearchCriteria criteria) {
        // Similar pattern for candidates
        List<Specification<CandidateProfile>> specs = new ArrayList<>();
        if (criteria.keywords() != null) specs.add(CandidateSpecification.withKeywords(criteria.keywords()));
        if (criteria.skills() != null && !criteria.skills().isEmpty())
            specs.add(CandidateSpecification.withSkills(criteria.skills()));
        if (criteria.minExperienceYears() != null)
            specs.add(CandidateSpecification.withExperienceYears(criteria.minExperienceYears()));
        if (criteria.availableFrom() != null)
            specs.add(CandidateSpecification.withAvailability(criteria.availableFrom()));
        if (criteria.location() != null)
            specs.add(CandidateSpecification.withLocation(criteria.location()));

        Specification<CandidateProfile> combined = CandidateSpecification.combine(specs);
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size());
        Page<CandidateProfile> results = candidateProfileRepository.findAll(combined, pageable);

        return results.map(cand -> {
            ScoredJobDTO.ScoreBreakdown breakdown = new ScoredJobDTO.ScoreBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            Double score = 0.0;
            return new ScoredCandidateDTO(cand.getId(), cand.getUserId(), cand.getJobDomain(), cand.getDesiredRole(),
                cand.getExperienceLevel(), cand.getLocation(), cand.getCity(), cand.getCountry(),
                cand.getSkills(), score, breakdown);
        });
    }

    public List<String> getSuggestions(String query) {
        // Top 5 job titles matching query
        return jobListingRepository.findAll(JobListingSpecification.withKeywords(query), PageRequest.of(0, 5))
            .map(JobListing::getTitle)
            .getContent();
    }
}
