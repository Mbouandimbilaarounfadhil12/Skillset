package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.Review;
import com.skillset.domain.port.ReviewRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String>, ReviewRepositoryPort {
    List<Review> findByApplicationId(String applicationId);
}
