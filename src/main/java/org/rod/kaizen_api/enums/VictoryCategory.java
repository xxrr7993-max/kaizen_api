package org.rod.kaizen_api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VictoryCategory {
    FISICA, MENTAL, ESPIRITUAL, PESSOAL;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static VictoryCategory from(String value) {
        return VictoryCategory.valueOf(value.toUpperCase());
    }
}
