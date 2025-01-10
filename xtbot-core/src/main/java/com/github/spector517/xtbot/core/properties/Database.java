package com.github.spector517.xtbot.core.properties;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Database(
        DatabaseType type,
        @JsonProperty("driver") String driverClassName,
        String url,
        String username,
        String password,
        @JsonProperty("dialect") String dialectClassName
) {}
