package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.CandidateProfile;
import com.skillset.domain.port.CandidateProfileRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, String>, JpaSpecificationExecutor<CandidateProfile>, CandidateProfileRepositoryPort {
    Optional<CandidateProfile> findByUserId(String userId);
    void deleteByUserId(String userId);
}
