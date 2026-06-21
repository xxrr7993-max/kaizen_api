package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.SubtaskCheckinModel;
import org.rod.kaizen_api.models.VictoryCheckinModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubtaskCheckinRepository extends JpaRepository<SubtaskCheckinModel, UUID> {
    Optional<SubtaskCheckinModel> findByVictoryCheckinAndSubtaskIndex(VictoryCheckinModel vc, int index);

    List<SubtaskCheckinModel> findAllByVictoryCheckin_VcId(UUID victoryCheckinVcId);
}
