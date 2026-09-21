package com.skillset.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDto {
    private long totalUsers;
    private long totalCandidates;
    private long totalEmployers;
    private long activeUsers;
    private long totalJobs;
    private long openJobs;
    private long totalApplications;
    private long pendingApplications;
    private long acceptedApplications;
    private long rejectedApplications;
    private List<UserSummary> recentUsers;
    private List<ActivityItem> recentActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private String id;
        private String name;
        private String email;
        private String role;
        private String status;
        private String joinedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String message;
        private String type;
        private String time;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobSummary {
        private String id;
        private String title;
        private String companyId;
        private String location;
        private String jobType;
        private String status;
        private String postedAt;
        private String expiresAt;
        private long applicationCount;
    }
}
