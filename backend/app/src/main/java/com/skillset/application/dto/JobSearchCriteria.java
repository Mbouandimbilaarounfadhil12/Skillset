package com.skillset.application.dto;

public record JobSearchCriteria(
    String keywords,
    String location,
    String contractType,
    Integer minSalary,
    Integer maxSalary,
    String experienceLevel,
    java.util.List<String> skills,
    Integer postedWithinDays,
    Integer page,
    Integer size,
    String sortBy
) {
    public JobSearchCriteria {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sortBy == null) sortBy = "RELEVANCE";
    }
}
