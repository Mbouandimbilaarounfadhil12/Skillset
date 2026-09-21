package com.skillset.infrastructure.persistence;

import com.skillset.domain.entity.User;
import com.skillset.domain.port.UserRepositoryPort;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserRepository repository;

    @Override
    public User saveUser(@NonNull User user) {
        return repository.save(user);
    }

    @Override
    public Optional<User> findUserById(@NonNull String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }
}
