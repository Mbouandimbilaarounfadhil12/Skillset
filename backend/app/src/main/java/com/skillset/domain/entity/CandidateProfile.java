package com.skillset.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    private String jobDomain;
    private String desiredRole;
    private String experienceLevel;
    private String contractType;
    private String country;
    private String city;
    private String desiredSalary;
    private String location;
    private String availability;
    private String remotePreference;

    @Column(columnDefinition = "text")
    private String skills;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "ai_answers", columnDefinition = "text")
    private String aiAnswers;

    @Column(name = "wants_cv", nullable = false)
    private Boolean wantsCv = false;

    @Column(name = "cv_url")
    private String cvUrl;

    @Column(name = "preferred_currency")
    private String preferredCurrency;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
