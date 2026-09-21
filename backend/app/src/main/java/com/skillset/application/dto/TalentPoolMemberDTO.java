package com.skillset.application.dto;

import com.skillset.domain.entity.TalentPoolMemberSource;
import com.skillset.domain.entity.TalentPoolMemberStatus;
import java.time.LocalDateTime;

public record TalentPoolMemberDTO(
    String id,
    String poolId,
    String candidateId,
    String candidateName,
    String candidateTitle,
    String candidateSkills,
    Double score,
    String notes,
    TalentPoolMemberStatus status,
    TalentPoolMemberSource source,
    LocalDateTime addedAt
) {}
