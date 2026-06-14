package org.rod.kaizen_api.exceptions;

import java.util.Map;

public record ErrorResponse(
        int code,
        String errorMessage,
        Map<String, String> errorDetails
) {}
