package org.rod.kaizen_api.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRecordDto(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 6) String newPassword
) {}
