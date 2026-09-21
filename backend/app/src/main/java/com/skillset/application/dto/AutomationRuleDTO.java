package com.skillset.application.dto;

import com.skillset.domain.entity.ApplicationStatus;
import com.skillset.domain.entity.AutomationRule.ActionType;
import com.skillset.domain.entity.AutomationRule.TriggerType;

public record AutomationRuleDTO(
    String id,
    String name,
    TriggerType triggerType,
    String triggerValue,
    ActionType actionType,
    String actionValue,
    ApplicationStatus targetStatus,
    boolean isActive,
    String createdAt
) {}
