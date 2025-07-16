package com.github.spector517.xtbot.core.properties.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DatabaseType {
    @JsonProperty("internal") INTERNAL,
    @JsonProperty("h2") H2
}
