package com.skillset.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "application_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;
    
    @ManyToOne
    @JoinColumn(name = "screening_question_id", nullable = false)
    private ScreeningQuestion screeningQuestion;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String answerText;
}
