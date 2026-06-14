package org.rod.kaizen_api.controllers;

import org.rod.kaizen_api.dtos.stats.HeatmapDataDto;
import org.rod.kaizen_api.dtos.stats.ProgressStatsDto;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.services.StatsService;
import org.rod.kaizen_api.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final StatsService statsService;
    private final UserService userService;

    public StatsController(StatsService statsService, UserService userService) {
        this.statsService = statsService;
        this.userService = userService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<ProgressStatsDto> getProgress(@AuthenticationPrincipal String userId,
                                                        @RequestParam(defaultValue = "6") int months) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(statsService.getProgress(user, months));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<HeatmapDataDto> getHeatmap(@AuthenticationPrincipal String userId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(statsService.getHeatmap(user));
    }
}
