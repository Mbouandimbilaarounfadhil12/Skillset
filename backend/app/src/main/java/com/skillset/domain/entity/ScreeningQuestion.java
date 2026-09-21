package com.skillset.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "screening_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "job_listing_id", nullable = false)
    private JobListing jobListing;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;
    
    @Column(nullable = false)
    private String questionType;
    
    @Column(columnDefinition = "TEXT")
    private String options;
    
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = true;
    
    @Column(name = "order_index")
    private Integer orderIndex;
}
