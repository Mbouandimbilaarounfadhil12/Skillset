package com.skillset.application.dto;

public record TalentPoolSummaryDTO(
    String id,
    String name,
    int totalMembers,
    int activeCount,
    int contactedCount,
    int hiredCount
) {}
