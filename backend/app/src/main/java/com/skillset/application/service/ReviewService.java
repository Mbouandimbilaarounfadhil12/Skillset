package com.skillset.application.service;

import com.skillset.application.dto.ReviewDTO;
import com.skillset.domain.entity.Application;
import com.skillset.domain.entity.Review;
import com.skillset.domain.port.ApplicationRepositoryPort;
import com.skillset.domain.port.ReviewRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepositoryPort reviewRepositoryPort;
    private final ApplicationRepositoryPort applicationRepositoryPort;

    public ReviewDTO addReview(String reviewerId, String applicationId,
                               Integer rating, String comments, String status) {
        Application application = applicationRepositoryPort.findById(applicationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidature introuvable"));
        Review review = new Review();
        review.setApplication(application);
        review.setReviewerId(reviewerId);
        review.setRating(rating);
        review.setComments(comments);
        review.setStatus(status != null ? status : "PENDING");
        return toDTO(reviewRepositoryPort.save(review));
    }

    public List<ReviewDTO> getReviews(String applicationId) {
        applicationRepositoryPort.findById(applicationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidature introuvable"));
        return reviewRepositoryPort.findByApplicationId(applicationId)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private ReviewDTO toDTO(Review r) {
        return new ReviewDTO(
            r.getId(),
            r.getApplication().getId(),
            r.getReviewerId(),
            r.getRating(),
            r.getComments(),
            r.getStatus(),
            r.getCreatedAt()
        );
    }
}
