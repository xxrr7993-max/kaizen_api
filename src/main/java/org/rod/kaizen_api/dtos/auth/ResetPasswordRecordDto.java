package org.rod.kaizen_api.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRecordDto(
        @NotBlank String token,
        @NotBlank @Size(min = 6) String newPassword
) {}
