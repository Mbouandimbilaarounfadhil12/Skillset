package com.skillset.domain.port;

import com.skillset.domain.entity.User;
import java.util.Optional;

public interface UserRepositoryPort {
    User saveUser(User user);
    Optional<User> findUserById(String id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
