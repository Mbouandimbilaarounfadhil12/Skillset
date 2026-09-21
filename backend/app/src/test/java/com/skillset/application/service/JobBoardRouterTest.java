package com.skillset.application.service;

import com.skillset.domain.entity.JobBoardConfig;
import com.skillset.domain.value.JobBoardPartner;
import com.skillset.infrastructure.persistence.JobBoardConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobBoardRouterTest {

    @Mock
    private JobBoardConfigRepository jobBoardConfigRepository;

    @InjectMocks
    private JobBoardRouter jobBoardRouter;

    @BeforeEach
    void setUp() {
        // Setup default mock behaviors
    }

    @Test
    void testDetermineTargetPartners_France() {
        // Arrange
        JobBoardConfig ftConfig = JobBoardConfig.builder()
            .countryCode("FR")
            .partner(JobBoardPartner.FRANCE_TRAVAIL)
            .isActive(true)
            .priority(1)
            .build();

        JobBoardConfig liConfig = JobBoardConfig.builder()
            .countryCode("FR")
            .partner(JobBoardPartner.LINKEDIN)
            .isActive(true)
            .priority(2)
            .build();

        when(jobBoardConfigRepository.findActiveByCountryWithFallback("FR"))
            .thenReturn(Arrays.asList(ftConfig, liConfig));

        // Act
        List<JobBoardPartner> result = jobBoardRouter.determineTargetPartners(Arrays.asList("FR"));

        // Assert
        assertEquals(2, result.size());
        assertEquals(JobBoardPartner.FRANCE_TRAVAIL, result.get(0));
        assertEquals(JobBoardPartner.LINKEDIN, result.get(1));
    }

    @Test
    void testDetermineTargetPartners_Cameroon() {
        // Arrange
        JobBoardConfig bmConfig = JobBoardConfig.builder()
            .countryCode("CM")
            .partner(JobBoardPartner.BRIGHTERMONDAY)
            .isActive(true)
            .priority(1)
            .build();

        JobBoardConfig liConfig = JobBoardConfig.builder()
            .countryCode("CM")
            .partner(JobBoardPartner.LINKEDIN)
            .isActive(true)
            .priority(2)
            .build();

        JobBoardConfig joConfig = JobBoardConfig.builder()
            .countryCode("CM")
            .partner(JobBoardPartner.JOBARTISAN)
            .isActive(true)
            .priority(3)
            .build();

        when(jobBoardConfigRepository.findActiveByCountryWithFallback("CM"))
            .thenReturn(Arrays.asList(bmConfig, liConfig, joConfig));

        // Act
        List<JobBoardPartner> result = jobBoardRouter.determineTargetPartners(Arrays.asList("CM"));

        // Assert
        assertEquals(3, result.size());
        assertEquals(JobBoardPartner.BRIGHTERMONDAY, result.get(0));
        assertEquals(JobBoardPartner.LINKEDIN, result.get(1));
        assertEquals(JobBoardPartner.JOBARTISAN, result.get(2));
    }

    @Test
    void testDetermineTargetPartners_MultipleContinents() {
        // Arrange - France
        JobBoardConfig frFtConfig = JobBoardConfig.builder()
            .countryCode("FR")
            .partner(JobBoardPartner.FRANCE_TRAVAIL)
            .isActive(true)
            .priority(1)
            .build();

        JobBoardConfig frLiConfig = JobBoardConfig.builder()
            .countryCode("FR")
            .partner(JobBoardPartner.LINKEDIN)
            .isActive(true)
            .priority(2)
            .build();

        // Arrange - Cameroon
        JobBoardConfig cmBmConfig = JobBoardConfig.builder()
            .countryCode("CM")
            .partner(JobBoardPartner.BRIGHTERMONDAY)
            .isActive(true)
            .priority(1)
            .build();

        JobBoardConfig cmLiConfig = JobBoardConfig.builder()
            .countryCode("CM")
            .partner(JobBoardPartner.LINKEDIN)
            .isActive(true)
            .priority(2)
            .build();

        when(jobBoardConfigRepository.findActiveByCountryWithFallback("FR"))
            .thenReturn(Arrays.asList(frFtConfig, frLiConfig));

        when(jobBoardConfigRepository.findActiveByCountryWithFallback("CM"))
            .thenReturn(Arrays.asList(cmBmConfig, cmLiConfig));

        // Act
        List<JobBoardPartner> result = jobBoardRouter.determineTargetPartners(Arrays.asList("FR", "CM"));

        // Assert
        assertEquals(3, result.size()); // FRANCE_TRAVAIL, BRIGHTERMONDAY, LINKEDIN (deduplicated)
        assertTrue(result.contains(JobBoardPartner.FRANCE_TRAVAIL));
        assertTrue(result.contains(JobBoardPartner.BRIGHTERMONDAY));
        assertTrue(result.contains(JobBoardPartner.LINKEDIN));
    }

    @Test
    void testDetermineTargetPartners_WithFallback() {
        // Arrange - No config for FR, fallback to universal (*)
        JobBoardConfig fallbackConfig = JobBoardConfig.builder()
            .countryCode("*")
            .partner(JobBoardPartner.LINKEDIN)
            .isActive(true)
            .priority(1)
            .build();

        when(jobBoardConfigRepository.findActiveByCountryWithFallback("XX"))
            .thenReturn(Arrays.asList(fallbackConfig));

        // Act
        List<JobBoardPartner> result = jobBoardRouter.determineTargetPartners(Arrays.asList("XX"));

        // Assert
        assertEquals(1, result.size());
        assertEquals(JobBoardPartner.LINKEDIN, result.get(0));
    }

    @Test
    void testDetermineTargetPartners_Empty() {
        // Arrange
        JobBoardConfig fallbackConfig = JobBoardConfig.builder()
            .countryCode("*")
            .partner(JobBoardPartner.LINKEDIN)
            .isActive(true)
            .priority(1)
            .build();

        when(jobBoardConfigRepository.findUniversalFallback())
            .thenReturn(Arrays.asList(fallbackConfig));

        // Act
        List<JobBoardPartner> result = jobBoardRouter.determineTargetPartners(Arrays.asList());

        // Assert
        assertEquals(1, result.size());
        assertEquals(JobBoardPartner.LINKEDIN, result.get(0));
    }

    @Test
    void testIsPartnerConfiguredForCountry() {
        // Arrange
        JobBoardConfig config = JobBoardConfig.builder()
            .countryCode("FR")
            .partner(JobBoardPartner.FRANCE_TRAVAIL)
            .isActive(true)
            .priority(1)
            .build();

        when(jobBoardConfigRepository.findByCountryCodeAndPartner("FR", JobBoardPartner.FRANCE_TRAVAIL))
            .thenReturn(Optional.of(config));

        when(jobBoardConfigRepository.findByCountryCodeAndPartner("FR", JobBoardPartner.BRIGHTERMONDAY))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertTrue(jobBoardRouter.isPartnerConfiguredForCountry("FR", JobBoardPartner.FRANCE_TRAVAIL));
        assertFalse(jobBoardRouter.isPartnerConfiguredForCountry("FR", JobBoardPartner.BRIGHTERMONDAY));
    }
}
