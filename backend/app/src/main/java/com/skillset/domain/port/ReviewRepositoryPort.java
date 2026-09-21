package com.skillset.domain.port;

import com.skillset.domain.entity.Review;
import java.util.List;

public interface ReviewRepositoryPort {
    Review save(Review review);
    List<Review> findByApplicationId(String applicationId);
}
