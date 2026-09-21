package com.skillset.domain.port;

import com.skillset.domain.entity.CandidateProfile;
import java.util.Optional;

public interface CandidateProfileRepositoryPort {
    CandidateProfile save(CandidateProfile profile);
    Optional<CandidateProfile> findByUserId(String userId);
}
