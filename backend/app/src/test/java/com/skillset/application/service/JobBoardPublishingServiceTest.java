package com.skillset.application.service;

import com.skillset.domain.entity.JobListing;
import com.skillset.domain.port.JobBoardAdapter;
import com.skillset.domain.value.JobBoardPartner;
import com.skillset.domain.value.JobBoardPublishResult;
import com.skillset.infrastructure.persistence.JobListingRepository;
import com.skillset.infrastructure.persistence.JobBoardPublicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobBoardPublishingServiceTest {

    @Mock
    private JobBoardRouter jobBoardRouter;

    @Mock
    private JobListingRepository jobListingRepository;

    @Mock
    private JobBoardPublicationRepository jobBoardPublicationRepository;

    @Mock
    private JobBoardAdapter franceTravailAdapter;

    @Mock
    private JobBoardAdapter linkedinAdapter;

    @Mock
    private JobBoardAdapter brighterMondayAdapter;

    private Map<JobBoardPartner, JobBoardAdapter> adapterRegistry;

    @InjectMocks
    private JobBoardPublishingService publishingService;

    @BeforeEach
    void setUp() {
        adapterRegistry = new HashMap<>();
        adapterRegistry.put(JobBoardPartner.FRANCE_TRAVAIL, franceTravailAdapter);
        adapterRegistry.put(JobBoardPartner.LINKEDIN, linkedinAdapter);
        adapterRegistry.put(JobBoardPartner.BRIGHTERMONDAY, brighterMondayAdapter);

        // Use reflection to inject the registry if needed
        // For simplicity, assuming the constructor or setter accepts it
    }

    @Test
    void testPublishToJobBoards_Success() throws Exception {
        // Arrange
        String jobId = "job-123";
        JobListing jobListing = new JobListing();
        jobListing.setId(jobId);
        jobListing.setTitle("Software Engineer");
        jobListing.setDescription("We are hiring");

        when(jobListingRepository.findById(jobId)).thenReturn(Optional.of(jobListing));
        when(jobBoardRouter.determineTargetPartners(Arrays.asList("FR")))
            .thenReturn(Arrays.asList(JobBoardPartner.FRANCE_TRAVAIL, JobBoardPartner.LINKEDIN));

        when(franceTravailAdapter.isAvailable()).thenReturn(true);
        when(linkedinAdapter.isAvailable()).thenReturn(true);

        when(franceTravailAdapter.publish(any(JobListing.class)))
            .thenReturn(JobBoardPublishResult.success(JobBoardPartner.FRANCE_TRAVAIL, "ft-001", "https://ft.com/001"));

        when(linkedinAdapter.publish(any(JobListing.class)))
            .thenReturn(JobBoardPublishResult.success(JobBoardPartner.LINKEDIN, "li-001", "https://linkedin.com/001"));

        // Act
        JobBoardPublishingService.PublishingReport report = publishingService.publishToJobBoards(jobId, Arrays.asList("FR"));

        // Assert
        assertEquals(jobId, report.getJobId());
        assertEquals(2, report.getResults().size());
        assertEquals(2, report.getSuccessCount());
        assertEquals(0, report.getFailureCount());
        assertTrue(report.allSuccess());

        verify(franceTravailAdapter).publish(any(JobListing.class));
        verify(linkedinAdapter).publish(any(JobListing.class));
    }

    @Test
    void testPublishToJobBoards_PartialFailure() throws Exception {
        // Arrange
        String jobId = "job-123";
        JobListing jobListing = new JobListing();
        jobListing.setId(jobId);

        when(jobListingRepository.findById(jobId)).thenReturn(Optional.of(jobListing));
        when(jobBoardRouter.determineTargetPartners(Arrays.asList("CM")))
            .thenReturn(Arrays.asList(JobBoardPartner.BRIGHTERMONDAY, JobBoardPartner.LINKEDIN));

        when(brighterMondayAdapter.isAvailable()).thenReturn(true);
        when(linkedinAdapter.isAvailable()).thenReturn(true);

        // BrighterMonday succeeds
        when(brighterMondayAdapter.publish(any(JobListing.class)))
            .thenReturn(JobBoardPublishResult.success(JobBoardPartner.BRIGHTERMONDAY, "bm-001", "https://bm.com/001"));

        // LinkedIn fails
        when(linkedinAdapter.publish(any(JobListing.class)))
            .thenReturn(JobBoardPublishResult.failure(JobBoardPartner.LINKEDIN, "API timeout"));

        // Act
        JobBoardPublishingService.PublishingReport report = publishingService.publishToJobBoards(jobId, Arrays.asList("CM"));

        // Assert
        assertEquals(2, report.getResults().size());
        assertEquals(1, report.getSuccessCount());
        assertEquals(1, report.getFailureCount());
        assertFalse(report.allSuccess());
        assertTrue(report.anySuccess());

        verify(brighterMondayAdapter).publish(any(JobListing.class));
        verify(linkedinAdapter).publish(any(JobListing.class));
    }

    @Test
    void testPublishToJobBoards_AllFailures_DoesNotBlockOthers() throws Exception {
        // Arrange
        String jobId = "job-123";
        JobListing jobListing = new JobListing();
        jobListing.setId(jobId);

        when(jobListingRepository.findById(jobId)).thenReturn(Optional.of(jobListing));
        when(jobBoardRouter.determineTargetPartners(Arrays.asList("FR", "CM")))
            .thenReturn(Arrays.asList(JobBoardPartner.FRANCE_TRAVAIL, JobBoardPartner.BRIGHTERMONDAY, JobBoardPartner.LINKEDIN));

        // All adapters fail, but each failure is independent
        when(franceTravailAdapter.isAvailable()).thenReturn(false);
        when(brighterMondayAdapter.isAvailable()).thenReturn(true);
        when(linkedinAdapter.isAvailable()).thenReturn(true);

        when(brighterMondayAdapter.publish(any(JobListing.class)))
            .thenReturn(JobBoardPublishResult.success(JobBoardPartner.BRIGHTERMONDAY, "bm-001", "https://bm.com/001"));

        when(linkedinAdapter.publish(any(JobListing.class)))
            .thenReturn(JobBoardPublishResult.success(JobBoardPartner.LINKEDIN, "li-001", "https://linkedin.com/001"));

        // Act
        JobBoardPublishingService.PublishingReport report = publishingService.publishToJobBoards(jobId, Arrays.asList("FR", "CM"));

        // Assert - FranceTravail unavailable doesn't block others
        assertEquals(3, report.getResults().size());
        assertEquals(2, report.getSuccessCount()); // BM + LinkedIn
        assertEquals(1, report.getFailureCount()); // FT unavailable

        verify(brighterMondayAdapter).publish(any(JobListing.class));
        verify(linkedinAdapter).publish(any(JobListing.class));
    }

    @Test
    void testPublishToJobBoards_JobNotFound() throws Exception {
        // Arrange
        String jobId = "nonexistent";
        when(jobListingRepository.findById(jobId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            publishingService.publishToJobBoards(jobId, Arrays.asList("FR"))
        );
    }
}
