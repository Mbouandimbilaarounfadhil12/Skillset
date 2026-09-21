package com.skillset.domain.port;

import com.skillset.domain.entity.JobListing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRepositoryPort extends JpaRepository<JobListing, String> {
    JobListing save(JobListing jobListing);
    //Optional<JobListing> findById(String id);
    //List<JobListing> findAll();
    List<JobListing> findByCompanyId(String companyId);
    List<JobListing> findByLocation(String location);
}
