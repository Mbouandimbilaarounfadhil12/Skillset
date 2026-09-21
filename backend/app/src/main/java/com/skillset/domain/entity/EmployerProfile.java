package com.skillset.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "employer_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    private String companyName;
    private String industry;
    private String companySize;
    private String hiringRole;
    private String companyCountry;
    private String companyCity;

    @Column(name = "contract_type")
    private String contractType;
    private String budget;
    private String benefits;
    private String remotePolicy;
    private String hiringTimeline;

    @Column(name = "ai_answers", columnDefinition = "text")
    private String aiAnswers;

    // Legitimacy & traceability fields
    @Column(name = "company_registration_number")
    private String companyRegistrationNumber;

    @Column(name = "company_website")
    private String companyWebsite;

    @Column(name = "company_linkedin")
    private String companyLinkedIn;

    @Column(name = "company_slug", unique = true)
    private String companySlug;

    @Column(name = "company_address")
    private String companyAddress;

    @Column(name = "company_latitude")
    private Double companyLatitude;

    @Column(name = "company_longitude")
    private Double companyLongitude;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
