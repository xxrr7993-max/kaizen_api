package org.rod.kaizen_api.dtos.checkin;

import org.rod.kaizen_api.enums.VictoryCategory;

import java.util.List;
import java.util.UUID;

public record VictoryCheckinDto(
        UUID victoryId,
        VictoryCategory category,
        boolean completed,
        List<SubtaskCheckinDto> subtasks
) {}
