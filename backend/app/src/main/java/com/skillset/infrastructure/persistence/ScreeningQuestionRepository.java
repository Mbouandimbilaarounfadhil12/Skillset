package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.ScreeningQuestion;
import com.skillset.domain.port.ScreeningQuestionRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreeningQuestionRepository extends JpaRepository<ScreeningQuestion, String>, ScreeningQuestionRepositoryPort {
    List<ScreeningQuestion> findByJobListingIdOrderByOrderIndex(String jobListingId);
}
