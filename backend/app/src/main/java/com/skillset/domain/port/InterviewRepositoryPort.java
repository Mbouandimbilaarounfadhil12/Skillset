package com.skillset.domain.port;

import com.skillset.domain.entity.Interview;
import java.util.List;
import java.util.Optional;

public interface InterviewRepositoryPort {
    Interview save(Interview interview);
    Optional<Interview> findById(String id);
    List<Interview> findByCandidateId(String candidateId);
    List<Interview> findByInterviewerId(String interviewerId);
    List<Interview> findByApplicationId(String applicationId);
}
