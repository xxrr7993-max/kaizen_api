package org.rod.kaizen_api.dtos.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRecordDto(@NotBlank String refreshToken) {}
