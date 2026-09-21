package com.skillset.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String jobSeekerId;
    
    @ManyToOne
    @JoinColumn(name = "job_listing_id", nullable = false)
    private JobListing jobListing;
    
    @Column(columnDefinition = "TEXT")
    private String coverLetter;
    
    @Column(name = "cv_url")
    private String cvUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;
    
    @Column(name = "match_score")
    private Double matchScore;

    @Column(name = "match_explanation", columnDefinition = "TEXT")
    private String matchExplanation;
    
    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt = LocalDateTime.now();
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    private List<ApplicationAnswer> answers;
    
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    private List<Review> reviews;
}

