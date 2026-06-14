package org.rod.kaizen_api.services;

import org.rod.kaizen_api.dtos.stats.HeatmapDataDto;
import org.rod.kaizen_api.dtos.stats.ProgressStatsDto;
import org.rod.kaizen_api.models.UserModel;

public interface StatsService {
    ProgressStatsDto getProgress(UserModel user, int months);
    HeatmapDataDto getHeatmap(UserModel user);
}
