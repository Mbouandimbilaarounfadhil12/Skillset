package com.skillset.domain.port;

import com.skillset.domain.entity.ScreeningQuestion;
import java.util.List;
import java.util.Optional;

public interface ScreeningQuestionRepositoryPort {
    ScreeningQuestion save(ScreeningQuestion question);
    Optional<ScreeningQuestion> findById(String id);
    List<ScreeningQuestion> findByJobListingId(String jobListingId);
    void deleteById(String id);
}
