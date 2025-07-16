package com.github.spector517.xtbot.core.properties.data;

public record Database(
        DatabaseType type,
        H2Properties h2
) {
    public record H2Properties(
            String directory
    ) {}
}
