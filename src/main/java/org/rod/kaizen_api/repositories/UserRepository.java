package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserModel, UUID> {
    boolean existsByEmail(String email);
    Optional<UserModel> findByEmail(String email);
    Optional<UserModel> findByPasswordResetToken(String token);
}
