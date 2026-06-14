package org.rod.kaizen_api.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRecordDto(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
