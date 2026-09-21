package com.skillset.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Interview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "application_id", nullable = false)
    private String applicationId;
    
    @Column(nullable = false)
    private String candidateId;
    
    @Column(nullable = false)
    private String interviewerId;
    
    @Column(nullable = false)
    private LocalDateTime scheduledAt;
    
    @Column(name = "interview_type")
    private String interviewType;
    
    @Column(name = "interview_link")
    private String interviewLink;

    @Column(name = "calendly_link")
    private String calendlyLink;
    
    @Column(name = "status")
    private String status;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "rating")
    private Integer rating;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "google_event_id")
    private String googleEventId;

    @Column(name = "calendar_sync_status")
    private String calendarSyncStatus;
}
