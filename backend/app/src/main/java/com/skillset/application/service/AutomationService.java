package com.skillset.application.service;

import com.skillset.application.dto.AutomationRuleDTO;
import com.skillset.domain.entity.*;
import com.skillset.domain.entity.AutomationRule.TriggerType;
import com.skillset.infrastructure.persistence.*;
import com.skillset.infrastructure.util.EmailUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationService {

    private final AutomationRuleRepository automationRuleRepository;
    private final ApplicationRepository applicationRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final UserRepository userRepository;
    private final EmailUtil emailUtil;
    private final NotificationPushService notificationPushService;

    // ── BULK STATUS UPDATE ────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> bulkUpdateStatus(List<String> applicationIds,
                                                 ApplicationStatus newStatus,
                                                 String employerId) {
        EmployerProfile profile = employerProfileRepository.findByUserId(employerId)
                .orElseThrow(() -> new RuntimeException("Profil employeur introuvable"));
        String companyId = profile.getId();

        List<Application> apps = applicationRepository.findByIdIn(applicationIds);

        List<String> toUpdate = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (Application app : apps) {
            if (companyId.equals(app.getJobListing().getCompanyId())) {
                toUpdate.add(app.getId());
            } else {
                skipped.add(app.getId());
            }
        }

        if (!toUpdate.isEmpty()) {
            applicationRepository.bulkUpdateStatus(toUpdate, newStatus, LocalDateTime.now());
            List<Application> updated = applicationRepository.findByIdIn(toUpdate);
            for (Application app : updated) {
                app.setStatus(newStatus);
                checkAndApplyRules(app, employerId);
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("updated", toUpdate.size());
        report.put("skipped", skipped);
        return report;
    }

    // ── RULE ENGINE ───────────────────────────────────────────────────────────

    public void checkAndApplyRules(Application app, String employerId) {
        List<AutomationRule> rules = automationRuleRepository.findByCreatedByAndIsActiveTrue(employerId);
        String currentStatus = app.getStatus().name();

        for (AutomationRule rule : rules) {
            if (rule.getTriggerType() != TriggerType.STATUS_CHANGED) continue;
            if (!currentStatus.equalsIgnoreCase(rule.getTriggerValue())) continue;

            log.info("Applying rule '{}' to application {}", rule.getName(), app.getId());
            executeAction(rule, app);
        }
    }

    private void executeAction(AutomationRule rule, Application app) {
        String candidateId = app.getJobSeekerId();
        String jobTitle = app.getJobListing().getTitle();

        userRepository.findById(candidateId).ifPresent(candidate -> {
            switch (rule.getActionType()) {
                case SEND_EMAIL -> emailUtil.sendAutomationTriggeredEmail(
                        candidate.getEmail(), candidate.getFirstName(), rule.getName(), jobTitle);
                case NOTIFY_CANDIDATE -> notificationPushService.push(
                        candidateId,
                        "automation",
                        "Mise à jour de votre candidature",
                        "La règle « " + rule.getName() + " » a été appliquée pour « " + jobTitle + " ».",
                        "/applications");
                case CHANGE_STATUS -> {
                    if (rule.getTargetStatus() != null) {
                        applicationRepository.bulkUpdateStatus(
                                List.of(app.getId()), rule.getTargetStatus(), LocalDateTime.now());
                        app.setStatus(rule.getTargetStatus());
                        app.setReviewedAt(LocalDateTime.now());
                    }
                }
            }
        });
    }

    // ── SCHEDULED RULES ───────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processScheduledRules() {
        List<AutomationRule> rules = automationRuleRepository
                .findByTriggerTypeAndIsActiveTrue(TriggerType.DAYS_WITHOUT_ACTION);

        List<ApplicationStatus> excluded = List.of(
                ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN,
                ApplicationStatus.APPROVED);

        for (AutomationRule rule : rules) {
            try {
                int days = Integer.parseInt(rule.getTriggerValue());
                LocalDateTime threshold = LocalDateTime.now().minusDays(days);

                EmployerProfile profile = employerProfileRepository
                        .findByUserId(rule.getCreatedBy()).orElse(null);
                if (profile == null) continue;

                List<Application> inactive = applicationRepository
                        .findInactiveApplications(profile.getId(), excluded, threshold);

                for (Application app : inactive) {
                    log.info("Scheduled rule '{}' applied to application {}", rule.getName(), app.getId());
                    executeAction(rule, app);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid triggerValue for rule {}: '{}'", rule.getId(), rule.getTriggerValue());
            }
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public AutomationRuleDTO createRule(AutomationRuleDTO dto, String employerId) {
        AutomationRule rule = new AutomationRule();
        rule.setName(dto.name());
        rule.setCreatedBy(employerId);
        rule.setTriggerType(dto.triggerType());
        rule.setTriggerValue(dto.triggerValue());
        rule.setActionType(dto.actionType());
        rule.setActionValue(dto.actionValue());
        rule.setTargetStatus(dto.targetStatus());
        rule.setActive(true);
        rule.setCreatedAt(LocalDateTime.now());
        return toDTO(automationRuleRepository.save(rule));
    }

    public AutomationRuleDTO toggleRule(String ruleId, String employerId) {
        AutomationRule rule = automationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Règle introuvable"));
        if (!employerId.equals(rule.getCreatedBy())) {
            throw new RuntimeException("Accès refusé");
        }
        rule.setActive(!rule.isActive());
        return toDTO(automationRuleRepository.save(rule));
    }

    public List<AutomationRuleDTO> getRulesForEmployer(String employerId) {
        return automationRuleRepository.findByCreatedBy(employerId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private AutomationRuleDTO toDTO(AutomationRule rule) {
        return new AutomationRuleDTO(
                rule.getId(),
                rule.getName(),
                rule.getTriggerType(),
                rule.getTriggerValue(),
                rule.getActionType(),
                rule.getActionValue(),
                rule.getTargetStatus(),
                rule.isActive(),
                rule.getCreatedAt() != null ? rule.getCreatedAt().toString() : null
        );
    }
}
