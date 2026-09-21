package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.AutomationRule;
import com.skillset.domain.entity.AutomationRule.TriggerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, String> {
    List<AutomationRule> findByCreatedBy(String employerId);
    List<AutomationRule> findByCreatedByAndIsActiveTrue(String employerId);
    List<AutomationRule> findByTriggerTypeAndIsActiveTrue(TriggerType type);
}
