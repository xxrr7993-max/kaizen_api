package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.RefreshTokenModel;
import org.rod.kaizen_api.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenModel, UUID> {
    Optional<RefreshTokenModel> findByToken(String token);
    void deleteByUser(UserModel user);
}
