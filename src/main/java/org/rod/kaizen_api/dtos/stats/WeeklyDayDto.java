package org.rod.kaizen_api.dtos.stats;

import java.util.Map;

public record WeeklyDayDto(String date, Map<String, Boolean> victories, int completedCount) {}
