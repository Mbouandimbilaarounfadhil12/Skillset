package com.skillset.application.dto;

public record CandidateSearchCriteria(
    String keywords,
    java.util.List<String> skills,
    Integer minExperienceYears,
    java.time.LocalDate availableFrom,
    String location,
    Double minMatchScore,
    Integer page,
    Integer size,
    String sortBy
) {
    public CandidateSearchCriteria {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sortBy == null) sortBy = "RELEVANCE";
    }
}
