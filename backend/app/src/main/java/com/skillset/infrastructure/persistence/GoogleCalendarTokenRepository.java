package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.GoogleCalendarToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoogleCalendarTokenRepository extends JpaRepository<GoogleCalendarToken, UUID> {
    Optional<GoogleCalendarToken> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
