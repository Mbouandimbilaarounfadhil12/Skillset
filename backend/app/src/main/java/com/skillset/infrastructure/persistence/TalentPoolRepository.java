package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.TalentPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TalentPoolRepository extends JpaRepository<TalentPool, String> {
    List<TalentPool> findByCreatedBy(String employerId);

    List<TalentPool> findByCreatedByAndJobListingId(String employerId, String jobListingId);
}
