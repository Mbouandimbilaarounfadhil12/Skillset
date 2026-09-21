package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, String> {
    Optional<UserPreferences> findByUserId(String userId);
    void deleteByUserId(String userId);
}
