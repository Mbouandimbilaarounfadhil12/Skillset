package com.skillset.application.dto;

public record ApplicationDTO(
    String id,
    String jobSeekerId,
    String jobListingId,
    String coverLetter,
    String cvUrl,
    String status,
    Double matchScore,
    String matchExplanation,
    String candidateName,
    String candidateEmail,
    String jobTitle,
    String candidatePhone,
    String companyId,
    String employerName,
    String employerPhone,
    String jobLocation,
    String jobType
) {}
