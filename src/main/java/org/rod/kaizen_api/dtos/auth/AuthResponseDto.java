package org.rod.kaizen_api.dtos.auth;

import org.rod.kaizen_api.dtos.UserProfileDto;

public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        UserProfileDto user
) {}
