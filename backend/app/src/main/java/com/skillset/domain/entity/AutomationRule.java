package com.skillset.domain.entity;

import com.skillset.domain.entity.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "automation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRule {

    public enum TriggerType { STATUS_CHANGED, DAYS_WITHOUT_ACTION, APPLICATION_RECEIVED }
    public enum ActionType { CHANGE_STATUS, SEND_EMAIL, NOTIFY_CANDIDATE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_by", nullable = false)
    private String createdBy;  // employerId

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private TriggerType triggerType;

    @Column(name = "trigger_value")
    private String triggerValue;  // "30" for days, or status name for STATUS_CHANGED

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Column(name = "action_value")
    private String actionValue;  // new status name or email template key

    @Enumerated(EnumType.STRING)
    @Column(name = "target_status")
    private ApplicationStatus targetStatus;  // nullable

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
