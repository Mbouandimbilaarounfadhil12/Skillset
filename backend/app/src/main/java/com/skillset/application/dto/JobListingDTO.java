package com.skillset.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobListingDTO {
    private String id;
    private String title;
    private String description;
    private String companyId;
    private String location;
    private String jobType;
    private String salaryMin;
    private String salaryMax;
    private String requiredSkills;
    private String responsibilities;
    private String status;

    // Champs enrichis pour l'affichage candidat (JobCard/JobSearch) — dérivés
    // côté serveur car ils nécessitent une jointure (EmployerProfile) ou un
    // calcul (nombre de candidatures, ancienneté) que le frontend ne peut pas
    // reconstituer à partir du seul companyId.
    private String company;
    private String sector;
    private String currency;
    private List<String> skills;
    private Boolean remote;
    private Integer postedDaysAgo;
    private Integer applicants;
    private Boolean featured;

    // Score de matching (0-100) pour le candidat courant — non nul uniquement
    // quand searchJobs() a pu classer les offres par pertinence (candidat avec
    // profil, tri 'relevance').
    private Double matchScore;
}
