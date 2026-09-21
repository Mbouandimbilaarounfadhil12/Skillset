package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.Application;
import com.skillset.domain.entity.ApplicationStatus;
import com.skillset.domain.port.ApplicationRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, String>, ApplicationRepositoryPort {
    List<Application> findByJobSeekerId(String jobSeekerId);
    void deleteByJobSeekerId(String jobSeekerId);
    List<Application> findByJobListingId(String jobListingId);
    long countByStatus(ApplicationStatus status);
    List<Application> findTop5ByOrderByAppliedAtDesc();
    List<Application> findByStatus(ApplicationStatus status);
    List<Application> findByAppliedAtAfter(LocalDateTime after);

    @Modifying
    @Query("UPDATE Application a SET a.status = :status, a.reviewedAt = :now WHERE a.id IN :ids")
    void bulkUpdateStatus(@Param("ids") List<String> ids,
                          @Param("status") ApplicationStatus status,
                          @Param("now") LocalDateTime now);

    List<Application> findByIdIn(List<String> ids);

    @Query("SELECT a FROM Application a WHERE a.jobListing.companyId = :companyId " +
           "AND a.status NOT IN :excludedStatuses " +
           "AND COALESCE(a.reviewedAt, a.appliedAt) < :threshold")
    List<Application> findInactiveApplications(@Param("companyId") String companyId,
                                               @Param("excludedStatuses") List<ApplicationStatus> excludedStatuses,
                                               @Param("threshold") LocalDateTime threshold);
}
