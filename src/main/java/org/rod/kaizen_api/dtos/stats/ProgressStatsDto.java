package org.rod.kaizen_api.dtos.stats;

import java.util.List;
import java.util.Map;

public record ProgressStatsDto(
        List<ProgressMonthDto> months,
        ProgressCurrentDto current,
        Map<String, Integer> completion
) {
    public record ProgressMonthDto(
            String label,
            int fisica,
            int mental,
            int espiritual,
            int pessoal,
            int overall
    ) {}

    public record ProgressCurrentDto(
            int streak,
            int record,
            long total,
            long activeDays
    ) {}
}
