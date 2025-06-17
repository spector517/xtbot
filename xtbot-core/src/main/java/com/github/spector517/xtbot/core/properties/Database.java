package com.github.spector517.xtbot.core.properties;

public record Database(
        DatabaseType type,
        H2Properties h2
) {
    public record H2Properties(
            String directory
    ) {}
}
