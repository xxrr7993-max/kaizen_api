package org.rod.kaizen_api.dtos.checkin;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record TodayCheckinDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate date,
        List<VictoryCheckinDto> victories,
        int completedCount,
        List<DiscardableTaskDto> discardable
) {}
