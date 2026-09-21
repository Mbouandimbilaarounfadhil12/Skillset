package com.skillset.domain.port;

import com.skillset.domain.entity.Application;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepositoryPort {
    Application save(Application application);
    Optional<Application> findById(String id);
    List<Application> findByJobSeekerId(String jobSeekerId);
    List<Application> findByJobListingId(String jobListingId);
}
