package com.skillset.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "job_listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobListing {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String companyId;
    
    @Column(nullable = false)
    private String location;
    
    @Column(nullable = false)
    private String jobType;
    
    @Column(nullable = false)
    private String salaryMin;
    
    @Column(nullable = false)
    private String salaryMax;
    
    @Column(columnDefinition = "TEXT")
    private String requiredSkills;
    
    @Column(columnDefinition = "TEXT")
    private String responsibilities;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.OPEN;
    
    @Column(name = "posted_at", nullable = false)
    private LocalDateTime postedAt = LocalDateTime.now();
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "jobListing", cascade = CascadeType.ALL)
    private List<Application> applications;
    
    @OneToMany(mappedBy = "jobListing", cascade = CascadeType.ALL)
    private List<ScreeningQuestion> screeningQuestions;

    @Column(name = "france_travail_id")
    private String franceTravailId;

    @Column(name = "published_on_france_travail")
    private Boolean publishedOnFranceTravail = false;

    @Column(name = "france_travail_url")
    private String franceTravailUrl;
}

