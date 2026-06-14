package org.rod.kaizen_api.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String name,
        String email,
        LocalDateTime createdAt,
        int streak,
        int record
) {}
