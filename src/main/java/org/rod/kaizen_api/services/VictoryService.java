package org.rod.kaizen_api.services;

import org.rod.kaizen_api.dtos.victory.VictoryGoalDto;
import org.rod.kaizen_api.dtos.victory.VictoryRecordDto;
import org.rod.kaizen_api.models.UserModel;

import java.util.List;
import java.util.UUID;

public interface VictoryService {
    List<VictoryRecordDto> findAll(UserModel user);
    List<VictoryRecordDto> saveAll(UserModel user, List<VictoryGoalDto> goals);
    VictoryRecordDto create(UserModel user, VictoryGoalDto goal);
    VictoryRecordDto update(UserModel user, UUID id, VictoryGoalDto goal);
    void delete(UserModel user, UUID id);
    void deleteAll(UserModel user);
}
