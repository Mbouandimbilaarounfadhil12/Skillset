package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.ApplicationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer, String> {
    List<ApplicationAnswer> findByApplicationId(String applicationId);
}
