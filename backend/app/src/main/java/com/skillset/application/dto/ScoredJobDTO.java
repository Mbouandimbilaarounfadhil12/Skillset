package com.skillset.application.dto;

public record ScoredJobDTO(
    String id,
    String title,
    String companyId,
    String location,
    String jobType,
    String salaryMin,
    String salaryMax,
    String requiredSkills,
    String description,
    java.time.LocalDateTime postedAt,
    java.time.LocalDateTime expiresAt,
    Double score,
    ScoreBreakdown breakdown
) {
    public record ScoreBreakdown(
        Double skillsPoints,
        Double experiencePoints,
        Double locationPoints,
        Double availabilityPoints,
        Double titlePoints,
        Double domainPoints
    ) {}
}
