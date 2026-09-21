package com.skillset.application.dto;

import java.util.List;

public record PublicCareersDto(
    String companySlug,
    String companyName,
    String industry,
    String companySize,
    String companyCity,
    String companyCountry,
    String companyWebsite,
    String companyLinkedIn,
    List<JobListingDTO> openJobs
) {}
