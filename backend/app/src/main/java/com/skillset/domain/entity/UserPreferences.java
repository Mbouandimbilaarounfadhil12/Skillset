package com.skillset.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false)
    private String userId;
    
    @Column(columnDefinition = "TEXT")
    private String preferredJobTypes;
    
    @Column(columnDefinition = "TEXT")
    private String preferredLocations;
    
    @Column(columnDefinition = "TEXT")
    private String preferredIndustries;
    
    @Column(name = "salary_expectation_min")
    private Double salaryExpectationMin;
    
    @Column(name = "salary_expectation_max")
    private Double salaryExpectationMax;
    
    @Column(name = "notifications_enabled", nullable = false)
    private Boolean notificationsEnabled = true;
    
    @Column(name = "email_alerts_enabled", nullable = false)
    private Boolean emailAlertsEnabled = true;
}
