package org.rod.kaizen_api.dtos.checkin;

import java.time.LocalDate;
import java.util.List;

public record TodayCheckinDto(
        LocalDate date,
        List<VictoryCheckinDto> victories,
        int completedCount,
        List<DiscardableTaskDto> discardable
) {}
