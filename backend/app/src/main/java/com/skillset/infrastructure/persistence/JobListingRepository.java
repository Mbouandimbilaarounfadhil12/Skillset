package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.JobListing;
import com.skillset.domain.entity.JobStatus;
import com.skillset.domain.port.JobRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobListingRepository extends JpaRepository<JobListing, String>, JpaSpecificationExecutor<JobListing> {//, JobRepositoryPort {
    List<JobListing> findByCompanyId(String companyId);
    List<JobListing> findByLocation(String location);
    long countByStatus(JobStatus status);
    List<JobListing> findTop5ByOrderByPostedAtDesc();
    List<JobListing> findByCompanyIdAndStatus(String companyId, JobStatus status);

    @Query("select j from JobListing j where j.id=?1")
    Optional<JobListing> findJobListingById(String id);

    @Modifying
    @Query("UPDATE JobListing j SET j.companyId = NULL WHERE j.companyId = :userId")
    void clearCompanyIdByUserId(@Param("userId") String userId);
}
