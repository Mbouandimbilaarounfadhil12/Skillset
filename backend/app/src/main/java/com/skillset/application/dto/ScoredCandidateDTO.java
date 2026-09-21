package com.skillset.application.dto;

public record ScoredCandidateDTO(
    String id,
    String userId,
    String jobDomain,
    String desiredRole,
    String experienceLevel,
    String location,
    String city,
    String country,
    String skills,
    Double score,
    ScoredJobDTO.ScoreBreakdown breakdown
) {}
