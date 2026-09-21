package com.skillset.application.dto;

import java.util.List;
import java.util.Map;

public record RecruitmentAnalyticsDto(
    Double avgTimeToHireHours,
    Map<String, Long> conversionFunnel,
    List<JobTimeToHire> topJobs,
    Map<String, Double> conversionRates,
    List<DailyCount> weeklyApplications
) {
    public record JobTimeToHire(String jobTitle, long applicationCount, Double avgTimeToHireHours) {}
    public record DailyCount(String date, long count) {}
}
