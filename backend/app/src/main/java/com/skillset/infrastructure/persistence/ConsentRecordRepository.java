package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.ConsentRecord;
import com.skillset.domain.entity.ConsentRecord.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, String> {
    List<ConsentRecord> findByUserId(String userId);
    Optional<ConsentRecord> findByUserIdAndConsentType(String userId, ConsentType consentType);
    List<ConsentRecord> findByUserIdAndRevokedAtIsNull(String userId);
    void deleteByUserId(String userId);
}
