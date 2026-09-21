package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.User;
import com.skillset.domain.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(UserRole role);
    long countByIsActive(Boolean isActive);
    List<User> findTop5ByOrderByCreatedAtDesc();
    List<User> findTop10ByOrderByCreatedAtDesc();
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String firstName, String lastName, String email);
}
