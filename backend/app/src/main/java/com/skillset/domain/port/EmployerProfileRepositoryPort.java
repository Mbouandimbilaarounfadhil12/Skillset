package com.skillset.domain.port;

import com.skillset.domain.entity.EmployerProfile;
import java.util.Optional;

public interface EmployerProfileRepositoryPort {
    EmployerProfile save(EmployerProfile profile);
    Optional<EmployerProfile> findByUserId(String userId);
}
