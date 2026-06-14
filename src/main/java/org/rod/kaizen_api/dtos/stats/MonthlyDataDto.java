package org.rod.kaizen_api.dtos.stats;

import java.util.Map;

public record MonthlyDataDto(String month, Map<String, Map<String, Boolean>> days) {}
