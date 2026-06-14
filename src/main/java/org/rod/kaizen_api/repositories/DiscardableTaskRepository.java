package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.CheckinModel;
import org.rod.kaizen_api.models.DiscardableTaskModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscardableTaskRepository extends JpaRepository<DiscardableTaskModel, UUID> {
    List<DiscardableTaskModel> findByCheckin(CheckinModel checkin);
}
