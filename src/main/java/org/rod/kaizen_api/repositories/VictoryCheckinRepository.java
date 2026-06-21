package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.CheckinModel;
import org.rod.kaizen_api.models.VictoryCheckinModel;
import org.rod.kaizen_api.models.VictoryModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VictoryCheckinRepository extends JpaRepository<VictoryCheckinModel, UUID> {
    Optional<VictoryCheckinModel> findByCheckinAndVictory(CheckinModel checkin, VictoryModel victory);
    List<VictoryCheckinModel> findByCheckin(CheckinModel checkin);

    List<VictoryCheckinModel> findAllByVictory(VictoryModel victory);
}
