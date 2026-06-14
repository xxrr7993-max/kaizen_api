package org.rod.kaizen_api.controllers;

import jakarta.validation.Valid;
import org.rod.kaizen_api.dtos.victory.VictoryGoalDto;
import org.rod.kaizen_api.dtos.victory.VictoryRecordDto;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.services.UserService;
import org.rod.kaizen_api.services.VictoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/victories")
public class VictoryController {

    private final VictoryService victoryService;
    private final UserService userService;

    public VictoryController(VictoryService victoryService, UserService userService) {
        this.victoryService = victoryService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<VictoryRecordDto>> findAll(@AuthenticationPrincipal String userId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(victoryService.findAll(user));
    }

    @PutMapping
    public ResponseEntity<List<VictoryRecordDto>> saveAll(@AuthenticationPrincipal String userId,
                                                          @Valid @RequestBody List<VictoryGoalDto> goals) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(victoryService.saveAll(user, goals));
    }

    @PostMapping
    public ResponseEntity<VictoryRecordDto> create(@AuthenticationPrincipal String userId,
                                                   @Valid @RequestBody VictoryGoalDto goal) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(victoryService.create(user, goal));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VictoryRecordDto> update(@AuthenticationPrincipal String userId,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody VictoryGoalDto goal) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(victoryService.update(user, id, goal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        UserModel user = userService.findById(UUID.fromString(userId));
        victoryService.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll(@AuthenticationPrincipal String userId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        victoryService.deleteAll(user);
        return ResponseEntity.noContent().build();
    }
}
