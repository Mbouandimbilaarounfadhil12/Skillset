package com.skillset.application.service;

import com.skillset.application.dto.AdminStatsDto;
import com.skillset.domain.entity.*;
import com.skillset.infrastructure.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.skillset.application.dto.RecruitmentAnalyticsDto;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final JobListingRepository jobListingRepository;

    private static final DateTimeFormatter FMT     = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter FMT_CSV = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private void requireAdmin(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux administrateurs");
        }
    }

    public AdminStatsDto getStats(String adminUserId) {
        requireAdmin(adminUserId);

        long totalUsers        = userRepository.count();
        long totalCandidates   = userRepository.countByRole(UserRole.CANDIDATE);
        long totalEmployers    = userRepository.countByRole(UserRole.EMPLOYER);
        long activeUsers       = userRepository.countByIsActive(true);
        long totalJobs         = jobListingRepository.count();
        long openJobs          = jobListingRepository.countByStatus(JobStatus.OPEN);
        long totalApps         = applicationRepository.count();
        long pendingApps       = applicationRepository.countByStatus(ApplicationStatus.SUBMITTED)
                               + applicationRepository.countByStatus(ApplicationStatus.SCREENING);
        long acceptedApps      = applicationRepository.countByStatus(ApplicationStatus.APPROVED);
        long rejectedApps      = applicationRepository.countByStatus(ApplicationStatus.REJECTED);

        List<AdminStatsDto.UserSummary> recentUsers = userRepository.findTop10ByOrderByCreatedAtDesc()
                .stream().map(this::toSummary).collect(Collectors.toList());

        return AdminStatsDto.builder()
                .totalUsers(totalUsers)
                .totalCandidates(totalCandidates)
                .totalEmployers(totalEmployers)
                .activeUsers(activeUsers)
                .totalJobs(totalJobs)
                .openJobs(openJobs)
                .totalApplications(totalApps)
                .pendingApplications(pendingApps)
                .acceptedApplications(acceptedApps)
                .rejectedApplications(rejectedApps)
                .recentUsers(recentUsers)
                .recentActivity(buildActivity())
                .build();
    }

    public List<AdminStatsDto.UserSummary> searchUsers(String adminUserId, String query) {
        requireAdmin(adminUserId);
        List<User> users = (query == null || query.isBlank())
                ? userRepository.findAll()
                : userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        query, query, query);
        return users.stream().map(this::toSummary).collect(Collectors.toList());
    }

    public void toggleUserStatus(String adminUserId, String targetUserId) {
        requireAdmin(adminUserId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        target.setIsActive(!Boolean.TRUE.equals(target.getIsActive()));
        userRepository.save(target);
    }

    private AdminStatsDto.UserSummary toSummary(User u) {
        return AdminStatsDto.UserSummary.builder()
                .id(u.getId())
                .name(u.getFirstName() + " " + u.getLastName())
                .email(u.getEmail())
                .role(u.getRole().name())
                .status(Boolean.TRUE.equals(u.getIsActive()) ? "ACTIVE" : "INACTIVE")
                .joinedAt(u.getCreatedAt() != null ? u.getCreatedAt().format(FMT) : "—")
                .build();
    }

    public RecruitmentAnalyticsDto getRecruitmentAnalytics(String adminUserId) {
        requireAdmin(adminUserId);

        // Conversion funnel — count by status
        Map<String, Long> funnel = new LinkedHashMap<>();
        funnel.put("SUBMITTED", applicationRepository.countByStatus(ApplicationStatus.SUBMITTED));
        funnel.put("SCREENING", applicationRepository.countByStatus(ApplicationStatus.SCREENING));
        funnel.put("INTERVIEW", applicationRepository.countByStatus(ApplicationStatus.INTERVIEW));
        funnel.put("OFFER",     applicationRepository.countByStatus(ApplicationStatus.OFFER));
        funnel.put("APPROVED",  applicationRepository.countByStatus(ApplicationStatus.APPROVED));
        funnel.put("REJECTED",  applicationRepository.countByStatus(ApplicationStatus.REJECTED));

        // Conversion rates between consecutive stages
        Map<String, Double> conversionRates = new LinkedHashMap<>();
        String[] stages = {"SUBMITTED", "SCREENING", "INTERVIEW", "OFFER", "APPROVED"};
        for (int i = 0; i < stages.length - 1; i++) {
            long from = funnel.getOrDefault(stages[i], 0L);
            long to   = funnel.getOrDefault(stages[i + 1], 0L);
            double rate = from == 0 ? 0.0 : Math.round((to * 1000.0) / from) / 10.0;
            conversionRates.put(stages[i] + "_TO_" + stages[i + 1], rate);
        }

        // Weekly trend: applications per day for the last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Application> recentApps = applicationRepository.findByAppliedAtAfter(sevenDaysAgo);
        Map<String, Long> perDay = new TreeMap<>();
        for (int i = 6; i >= 0; i--) {
            perDay.put(LocalDate.now().minusDays(i)
                .format(DateTimeFormatter.ofPattern("dd/MM")), 0L);
        }
        recentApps.forEach(a -> {
            if (a.getAppliedAt() != null) {
                String key = a.getAppliedAt().toLocalDate()
                    .format(DateTimeFormatter.ofPattern("dd/MM"));
                perDay.merge(key, 1L, (a1, b) -> a1 + b);
            }
        });
        List<RecruitmentAnalyticsDto.DailyCount> weeklyApplications = perDay.entrySet().stream()
            .map(e -> new RecruitmentAnalyticsDto.DailyCount(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

        // Time-to-hire: appliedAt → reviewedAt for APPROVED applications
        List<Application> approvedApps = applicationRepository.findByStatus(ApplicationStatus.APPROVED);
        List<Double> durations = approvedApps.stream()
            .filter(a -> a.getAppliedAt() != null && a.getReviewedAt() != null)
            .map(a -> (double) Duration.between(a.getAppliedAt(), a.getReviewedAt()).toHours())
            .collect(Collectors.toList());

        Double avgTimeToHire = durations.isEmpty() ? null
            : durations.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        // Top jobs by approved-application count
        Map<String, List<Application>> byJob = approvedApps.stream()
            .filter(a -> a.getJobListing() != null)
            .collect(Collectors.groupingBy(a -> a.getJobListing().getTitle()));

        List<RecruitmentAnalyticsDto.JobTimeToHire> topJobs = byJob.entrySet().stream()
            .map(e -> {
                List<Application> list = e.getValue();
                OptionalDouble avg = list.stream()
                    .filter(a -> a.getAppliedAt() != null && a.getReviewedAt() != null)
                    .mapToDouble(a -> Duration.between(a.getAppliedAt(), a.getReviewedAt()).toHours())
                    .average();
                return new RecruitmentAnalyticsDto.JobTimeToHire(
                    e.getKey(), list.size(), avg.isPresent() ? avg.getAsDouble() : null);
            })
            .sorted(Comparator.comparingLong(RecruitmentAnalyticsDto.JobTimeToHire::applicationCount).reversed())
            .limit(5)
            .collect(Collectors.toList());

        return new RecruitmentAnalyticsDto(avgTimeToHire, funnel, topJobs, conversionRates, weeklyApplications);
    }

    public List<AdminStatsDto.JobSummary> listAllJobs(String adminUserId, String status) {
        requireAdmin(adminUserId);
        List<JobListing> jobs = (status == null || status.isBlank())
                ? jobListingRepository.findAll()
                : jobListingRepository.findAll().stream()
                    .filter(j -> j.getStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        return jobs.stream().map(this::toJobSummary).collect(Collectors.toList());
    }

    public AdminStatsDto.JobSummary changeJobStatus(String adminUserId, String jobId, String newStatus) {
        requireAdmin(adminUserId);
        JobListing job = jobListingRepository.findJobListingById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));
        job.setStatus(JobStatus.valueOf(newStatus.toUpperCase()));
        return toJobSummary(jobListingRepository.save(job));
    }

    private AdminStatsDto.JobSummary toJobSummary(JobListing j) {
        long appCount = j.getApplications() != null ? j.getApplications().size() : 0;
        return AdminStatsDto.JobSummary.builder()
                .id(j.getId())
                .title(j.getTitle())
                .companyId(j.getCompanyId())
                .location(j.getLocation())
                .jobType(j.getJobType())
                .status(j.getStatus().name())
                .postedAt(j.getPostedAt() != null ? j.getPostedAt().format(FMT_CSV) : "—")
                .expiresAt(j.getExpiresAt() != null ? j.getExpiresAt().format(FMT_CSV) : null)
                .applicationCount(appCount)
                .build();
    }

    public byte[] exportApplicationsCsv(String adminUserId) {
        requireAdmin(adminUserId);
        List<Application> all = applicationRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Candidat,Email,Poste,Statut,Score,Date candidature,Date décision\n");
        for (Application a : all) {
            String name  = "";
            String email = "";
            Optional<User> u = userRepository.findById(a.getJobSeekerId());
            if (u.isPresent()) { name = u.get().getFirstName() + " " + u.get().getLastName(); email = u.get().getEmail(); }
            sb.append(csv(a.getId())).append(',')
              .append(csv(name)).append(',')
              .append(csv(email)).append(',')
              .append(csv(a.getJobListing() != null ? a.getJobListing().getTitle() : "")).append(',')
              .append(csv(a.getStatus().name())).append(',')
              .append(a.getMatchScore() != null ? a.getMatchScore() : "").append(',')
              .append(a.getAppliedAt() != null ? a.getAppliedAt().format(FMT_CSV) : "").append(',')
              .append(a.getReviewedAt() != null ? a.getReviewedAt().format(FMT_CSV) : "").append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    private List<AdminStatsDto.ActivityItem> buildActivity() {
        List<AdminStatsDto.ActivityItem> items = new ArrayList<>();

        userRepository.findTop5ByOrderByCreatedAtDesc().forEach(u ->
            items.add(AdminStatsDto.ActivityItem.builder()
                .message((u.getRole() == UserRole.CANDIDATE
                        ? "Nouveau candidat inscrit : "
                        : u.getRole() == UserRole.EMPLOYER
                            ? "Nouvel employeur inscrit : "
                            : "Nouvel admin : ")
                        + u.getFirstName() + " " + u.getLastName())
                .type("user")
                .time(u.getCreatedAt() != null ? u.getCreatedAt().format(FMT) : "—")
                .build())
        );

        applicationRepository.findTop5ByOrderByAppliedAtDesc().forEach(a ->
            items.add(AdminStatsDto.ActivityItem.builder()
                .message("Candidature soumise — " +
                        (a.getJobListing() != null ? a.getJobListing().getTitle() : "offre inconnue"))
                .type("file")
                .time(a.getAppliedAt() != null ? a.getAppliedAt().format(FMT) : "—")
                .build())
        );

        jobListingRepository.findTop5ByOrderByPostedAtDesc().forEach(j ->
            items.add(AdminStatsDto.ActivityItem.builder()
                .message("Offre publiée : " + j.getTitle())
                .type("job")
                .time(j.getPostedAt() != null ? j.getPostedAt().format(FMT) : "—")
                .build())
        );

        return items.stream().limit(10).collect(Collectors.toList());
    }
}
