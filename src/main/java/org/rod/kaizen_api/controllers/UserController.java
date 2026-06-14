package org.rod.kaizen_api.controllers;

import org.rod.kaizen_api.dtos.UserProfileDto;
import org.rod.kaizen_api.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileDto> updateProfile(@AuthenticationPrincipal String userId,
                                                        @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.updateProfile(userId, body.get("name"), body.get("email")));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal String userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }
}
