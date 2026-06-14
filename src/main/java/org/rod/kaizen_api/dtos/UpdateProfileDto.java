package org.rod.kaizen_api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileDto(
        @Size(max = 100) String name,
        @Email @Size(max = 150) String email
) {}
