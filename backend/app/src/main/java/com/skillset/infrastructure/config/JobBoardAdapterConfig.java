package com.skillset.infrastructure.config;

import com.skillset.domain.port.JobBoardAdapter;
import com.skillset.domain.value.JobBoardPartner;
import com.skillset.infrastructure.integration.adapter.BrighterMondayAdapter;
import com.skillset.infrastructure.integration.adapter.FranceTravailAdapter;
import com.skillset.infrastructure.integration.adapter.JobartisanAdapter;
import com.skillset.infrastructure.integration.adapter.LinkedInAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Spring pour l'enregistrement des adaptateurs job board.
 * Crée une Map<JobBoardPartner, JobBoardAdapter> injecté dans JobBoardPublishingService.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class JobBoardAdapterConfig {

    private final FranceTravailAdapter franceTravailAdapter;
    private final LinkedInAdapter linkedInAdapter;
    private final BrighterMondayAdapter brighterMondayAdapter;
    private final JobartisanAdapter jobartisanAdapter;

    /**
     * Crée une Map des adaptateurs indexée par JobBoardPartner.
     * Utilisée pour les lookups dynamiques lors de la publication.
     *
     * @return Map des adaptateurs
     */
    @Bean
    public Map<JobBoardPartner, JobBoardAdapter> jobBoardAdapterRegistry() {
        Map<JobBoardPartner, JobBoardAdapter> registry = new HashMap<>();

        registry.put(JobBoardPartner.FRANCE_TRAVAIL, franceTravailAdapter);
        registry.put(JobBoardPartner.LINKEDIN, linkedInAdapter);
        registry.put(JobBoardPartner.BRIGHTERMONDAY, brighterMondayAdapter);
        registry.put(JobBoardPartner.JOBARTISAN, jobartisanAdapter);

        log.info("Registered {} job board adapters", registry.size());
        registry.forEach((partner, adapter) -> {
            boolean available = adapter.isAvailable();
            log.info("  {} - available: {}", partner, available);
        });

        return registry;
    }
}
