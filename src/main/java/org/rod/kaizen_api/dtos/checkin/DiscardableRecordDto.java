package org.rod.kaizen_api.dtos.checkin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiscardableRecordDto(@NotBlank @Size(max = 200) String goal) {}
