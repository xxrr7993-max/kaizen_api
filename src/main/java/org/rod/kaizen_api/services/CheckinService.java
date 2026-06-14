package org.rod.kaizen_api.services;

import org.rod.kaizen_api.dtos.checkin.*;
import org.rod.kaizen_api.dtos.stats.MonthlyDataDto;
import org.rod.kaizen_api.dtos.stats.StreakDto;
import org.rod.kaizen_api.dtos.stats.WeeklyDayDto;
import org.rod.kaizen_api.models.UserModel;

import java.util.List;
import java.util.UUID;

public interface CheckinService {
    TodayCheckinDto getToday(UserModel user);
    TodayCheckinDto toggleVictory(UserModel user, UUID victoryId);
    TodayCheckinDto toggleSubtask(UserModel user, UUID victoryId, int subtaskIndex);
    StreakDto getStreak(UserModel user);
    List<WeeklyDayDto> getWeekly(UserModel user);
    MonthlyDataDto getMonthly(UserModel user, String month);
    TodayCheckinDto addDiscardable(UserModel user, DiscardableRecordDto dto);
    TodayCheckinDto toggleDiscardable(UserModel user, UUID taskId);
    TodayCheckinDto deleteDiscardable(UserModel user, UUID taskId);
}
