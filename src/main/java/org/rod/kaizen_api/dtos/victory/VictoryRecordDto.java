package org.rod.kaizen_api.dtos.victory;

import org.rod.kaizen_api.enums.VictoryCategory;

import java.util.List;
import java.util.UUID;

public record VictoryRecordDto(
        UUID id,
        VictoryCategory category,
        String goal,
        List<String> subtasks,
        int order
) {}
