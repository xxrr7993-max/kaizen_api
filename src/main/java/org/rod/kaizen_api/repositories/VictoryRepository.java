package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.enums.VictoryCategory;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.models.VictoryModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VictoryRepository extends JpaRepository<VictoryModel, UUID> {
    List<VictoryModel> findByUserOrderByOrderAsc(UserModel user);
    void deleteByUser(UserModel user);
    Optional<VictoryModel> findByVictoryIdAndUser(UUID id, UserModel user);
    boolean existsByUserAndCategory(UserModel user, VictoryCategory category);
}
