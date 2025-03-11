package com.github.spector517.xtbot.core.properties;

import java.util.List;

public record MessageProps(
        String id,
        String text,
        ParseModeProps parseMode,
        List<List<ButtonProps>> buttons
) {}
