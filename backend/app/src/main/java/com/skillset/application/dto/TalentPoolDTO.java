package com.skillset.application.dto;

import java.time.LocalDateTime;

public record TalentPoolDTO(
    String id,
    String name,
    String description,
    String jobListingId,
    boolean isPublic,
    LocalDateTime createdAt
) {}
