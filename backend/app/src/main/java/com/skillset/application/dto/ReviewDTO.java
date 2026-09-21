package com.skillset.application.dto;

import java.time.LocalDateTime;

public record ReviewDTO(
    String id,
    String applicationId,
    String reviewerId,
    Integer rating,
    String comments,
    String status,
    LocalDateTime createdAt
) {}
