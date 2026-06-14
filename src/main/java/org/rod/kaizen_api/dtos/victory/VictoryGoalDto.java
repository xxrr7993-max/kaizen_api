package org.rod.kaizen_api.dtos.victory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.rod.kaizen_api.enums.VictoryCategory;

import java.util.List;

public record VictoryGoalDto(
        @NotNull VictoryCategory category,
        @NotBlank @Size(max = 200) String goal,
        List<String> subtasks
) {}
