package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.Interview;
import com.skillset.domain.port.InterviewRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, String>, InterviewRepositoryPort {
    List<Interview> findByCandidateId(String candidateId);
    void deleteByCandidateId(String candidateId);
    List<Interview> findByInterviewerId(String interviewerId);
    List<Interview> findByApplicationId(String applicationId);
}
