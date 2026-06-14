package org.rod.kaizen_api.dtos.checkin;

import java.util.UUID;

public record DiscardableTaskDto(UUID id, String goal, boolean completed) {}
