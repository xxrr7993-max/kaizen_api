package org.rod.kaizen_api.controllers;

import jakarta.validation.Valid;
import org.rod.kaizen_api.dtos.checkin.*;
import org.rod.kaizen_api.dtos.stats.MonthlyDataDto;
import org.rod.kaizen_api.dtos.stats.StreakDto;
import org.rod.kaizen_api.dtos.stats.WeeklyDayDto;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.services.CheckinService;
import org.rod.kaizen_api.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/checkin")
public class CheckinController {

    private final CheckinService checkinService;
    private final UserService userService;

    public CheckinController(CheckinService checkinService, UserService userService) {
        this.checkinService = checkinService;
        this.userService = userService;
    }

    @GetMapping("/today")
    public ResponseEntity<TodayCheckinDto> getToday(@AuthenticationPrincipal String userId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.getToday(user));
    }

    @PostMapping("/victory/{victoryId}/toggle")
    public ResponseEntity<TodayCheckinDto> toggleVictory(@AuthenticationPrincipal String userId,
                                                         @PathVariable UUID victoryId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.toggleVictory(user, victoryId));
    }

    @PostMapping("/victory/{victoryId}/subtask/{subtaskIndex}/toggle")
    public ResponseEntity<TodayCheckinDto> toggleSubtask(@AuthenticationPrincipal String userId,
                                                         @PathVariable UUID victoryId,
                                                         @PathVariable int subtaskIndex) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.toggleSubtask(user, victoryId, subtaskIndex));
    }

    @GetMapping("/streak")
    public ResponseEntity<StreakDto> getStreak(@AuthenticationPrincipal String userId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.getStreak(user));
    }

    @GetMapping("/week")
    public ResponseEntity<List<WeeklyDayDto>> getWeekly(@AuthenticationPrincipal String userId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.getWeekly(user));
    }

    @GetMapping("/month")
    public ResponseEntity<MonthlyDataDto> getMonthly(@AuthenticationPrincipal String userId,
                                                     @RequestParam String m) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.getMonthly(user, m));
    }

    @PostMapping("/discardable")
    public ResponseEntity<TodayCheckinDto> addDiscardable(@AuthenticationPrincipal String userId,
                                                          @Valid @RequestBody DiscardableRecordDto dto) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.addDiscardable(user, dto));
    }

    @PostMapping("/discardable/{id}/toggle")
    public ResponseEntity<TodayCheckinDto> toggleDiscardable(@AuthenticationPrincipal String userId,
                                                             @PathVariable UUID id) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.toggleDiscardable(user, id));
    }

    @DeleteMapping("/discardable/{id}")
    public ResponseEntity<TodayCheckinDto> deleteDiscardable(@AuthenticationPrincipal String userId,
                                                             @PathVariable UUID id) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.deleteDiscardable(user, id));
    }
}
