package com.skillset.infrastructure.scheduler;

import com.skillset.domain.entity.JobStatus;
import com.skillset.infrastructure.persistence.JobListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Hourly job that closes any JobListing whose expiresAt is in the past.
 * Requires @EnableScheduling on the main application class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobCleanupScheduler {

    private final JobListingRepository jobListingRepository;

    @Scheduled(cron = "0 0 * * * *")   // every hour, on the hour
    public void closeExpiredListings() {
        LocalDateTime now = LocalDateTime.now();
        int count = jobListingRepository.findAll().stream()
            .filter(j -> j.getStatus() == JobStatus.OPEN
                      && j.getExpiresAt() != null
                      && j.getExpiresAt().isBefore(now))
            .mapToInt(j -> {
                j.setStatus(JobStatus.CLOSED);
                jobListingRepository.save(j);
                return 1;
            })
            .sum();
        if (count > 0) log.info("JobCleanup: {} offre(s) expirée(s) fermée(s)", count);
    }
}
