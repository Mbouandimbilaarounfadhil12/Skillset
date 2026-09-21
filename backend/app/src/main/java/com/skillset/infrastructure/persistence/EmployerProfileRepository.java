package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.EmployerProfile;
import com.skillset.domain.port.EmployerProfileRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployerProfileRepository extends JpaRepository<EmployerProfile, String>, EmployerProfileRepositoryPort {
    Optional<EmployerProfile> findByUserId(String userId);
    Optional<EmployerProfile> findByCompanySlug(String companySlug);
    void deleteByUserId(String userId);
}
