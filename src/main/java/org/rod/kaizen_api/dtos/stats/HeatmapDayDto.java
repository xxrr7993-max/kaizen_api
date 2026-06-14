package org.rod.kaizen_api.dtos.stats;

import java.util.Map;

public record HeatmapDayDto(String date, Map<String, Boolean> victories) {}
